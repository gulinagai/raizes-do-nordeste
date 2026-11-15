package br.edu.uninter.raizes.application.servico;


import br.edu.uninter.raizes.domain.entidade.Estoque;
import br.edu.uninter.raizes.domain.entidade.ItemPedido;
import br.edu.uninter.raizes.domain.entidade.Pedido;
import br.edu.uninter.raizes.domain.entidade.Produto;
import br.edu.uninter.raizes.domain.entidade.Unidade;
import br.edu.uninter.raizes.domain.entidade.Usuario;
import br.edu.uninter.raizes.domain.enumeracao.CanalPedido;
import br.edu.uninter.raizes.domain.enumeracao.Perfil;
import br.edu.uninter.raizes.domain.enumeracao.StatusPedido;
import br.edu.uninter.raizes.domain.enumeracao.TipoMovimentacaoEstoque;
import br.edu.uninter.raizes.domain.excecao.AcessoNegadoException;
import br.edu.uninter.raizes.domain.excecao.EstoqueInsuficienteException;
import br.edu.uninter.raizes.domain.excecao.ParametrosInvalidosException;
import br.edu.uninter.raizes.domain.excecao.ParametrosInvalidosException.Campo;
import br.edu.uninter.raizes.domain.excecao.RecursoInativoException;
import br.edu.uninter.raizes.domain.excecao.RecursoNaoEncontradoException;
import br.edu.uninter.raizes.domain.excecao.ReservaExcedidaException;
import br.edu.uninter.raizes.infrastructure.persistencia.EstoqueRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.PedidoRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.ProdutoRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UnidadeRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoServico {

    private static final int LIMITE_MAXIMO = 50;

    private final UnidadeRepositorio unidades;
    private final UsuarioRepositorio usuarios;
    private final ProdutoRepositorio produtos;
    private final EstoqueRepositorio estoques;
    private final PedidoRepositorio pedidos;
    private final MovimentacaoEstoqueServico movimentacoes;

    public PedidoServico(
            UnidadeRepositorio unidades,
            UsuarioRepositorio usuarios,
            ProdutoRepositorio produtos,
            EstoqueRepositorio estoques,
            PedidoRepositorio pedidos,
            MovimentacaoEstoqueServico movimentacoes) {
        this.unidades = unidades;
        this.usuarios = usuarios;
        this.produtos = produtos;
        this.estoques = estoques;
        this.pedidos = pedidos;
        this.movimentacoes = movimentacoes;
    }

    @Transactional
    public ResultadoPedido criar(
            Long usuarioId, Perfil perfil, Long unidadeId, Long clienteId, CanalPedido canalPedido, List<ItemSolicitado> itens) {
        if (perfil == Perfil.CLIENTE && !usuarioId.equals(clienteId)) {
            throw new AcessoNegadoException();
        }
        Unidade unidade = unidades.findById(unidadeId).orElseThrow(RecursoNaoEncontradoException::new);
        if (!unidade.isAtivo()) {
            throw new RecursoInativoException("unidadeId");
        }
        Usuario cliente = usuarios.findById(clienteId)
                .filter(usuario -> usuario.getPerfil() == Perfil.CLIENTE)
                .orElseThrow(RecursoNaoEncontradoException::new);

        List<Reserva> reservas = reservar(unidade, itens);
        Pedido pedido = new Pedido(unidade, cliente, canalPedido, Instant.now().truncatedTo(ChronoUnit.SECONDS));
        BigDecimal total = BigDecimal.ZERO;
        for (Reserva reserva : reservas) {
            Estoque estoque = reserva.estoque();
            estoque.setQuantidadeReservada(estoque.getQuantidadeReservada() + reserva.quantidade());
            BigDecimal preco = reserva.produto().getPreco();
            total = total.add(preco.multiply(BigDecimal.valueOf(reserva.quantidade())));
            pedido.adicionarItem(new ItemPedido(pedido, reserva.produto(), reserva.quantidade(), preco));
        }
        pedido.definirValorTotal(total);
        Pedido salvo = pedidos.saveAndFlush(pedido);
        return resultado(salvo);
    }

    @Transactional(readOnly = true)
    public ResultadoListaPedidos listar(
            Long usuarioId, Perfil perfil, String canalPedido, String status, int pagina, int limite) {
        List<Campo> campos = new ArrayList<>();
        if (pagina < 1) {
            campos.add(new Campo("page", "deve ser maior ou igual a 1"));
        }
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            campos.add(new Campo("limit", "deve estar entre 1 e 50"));
        }
        CanalPedido canal = enumOuNulo(canalPedido, CanalPedido.class, "canalPedido", campos);
        StatusPedido situacao = enumOuNulo(status, StatusPedido.class, "status", campos);
        if (!campos.isEmpty()) {
            throw new ParametrosInvalidosException(campos);
        }

        Long clienteId = perfil == Perfil.CLIENTE ? usuarioId : null;
        Page<Pedido> consulta = pedidos.listar(
                clienteId,
                canal,
                situacao,
                PageRequest.of(pagina - 1, limite, Sort.by(Sort.Order.desc("dataCriacao"), Sort.Order.desc("id"))));
        List<ResultadoPedido> itens = carregar(consulta.getContent()).stream().map(this::resultado).toList();
        return new ResultadoListaPedidos(
                itens, pagina, limite, consulta.getTotalElements(), consulta.getTotalPages());
    }

    @Transactional
    public ResultadoPedido atualizarStatus(Long id, StatusPedido status, Perfil perfil) {
        if ((perfil == Perfil.COZINHA && status != StatusPedido.PRONTO)
                || (perfil == Perfil.ATENDENTE && status != StatusPedido.ENTREGUE)) {
            throw new AcessoNegadoException();
        }
        Pedido pedido = pedidos.findById(id).orElseThrow(RecursoNaoEncontradoException::new);
        pedido.avancarStatus(status);
        return resultado(pedidos.saveAndFlush(pedido));
    }

    @Transactional
    public ResultadoPedido cancelar(Long id, Long usuarioId, Perfil perfil) {
        Pedido pedido = pedidos.findById(id).orElseThrow(RecursoNaoEncontradoException::new);
        exigirDono(pedido, usuarioId, perfil);
        if (pedido.getStatus() == StatusPedido.AGUARDANDO_PAGAMENTO) {
            liberarReserva(pedido);
        } else if (pedido.getStatus() == StatusPedido.EM_PREPARO || pedido.getStatus() == StatusPedido.PRONTO) {
            devolverEstoque(pedido);
        }
        pedido.cancelar();
        return resultado(pedidos.saveAndFlush(pedido));
    }

    private void exigirDono(Pedido pedido, Long usuarioId, Perfil perfil) {
        if (perfil == Perfil.CLIENTE && !pedido.getCliente().getId().equals(usuarioId)) {
            throw new AcessoNegadoException();
        }
    }

    private void liberarReserva(Pedido pedido) {
        for (Map.Entry<Long, Integer> item : quantidades(pedido).entrySet()) {
            Estoque estoque = estoques.findByUnidadeIdAndProdutoId(pedido.getUnidade().getId(), item.getKey())
                    .orElseThrow(() -> new EstoqueInsuficienteException(0));
            if (estoque.getQuantidadeReservada() < item.getValue()) {
                throw new EstoqueInsuficienteException(estoque.getQuantidadeReservada());
            }
            estoque.setQuantidadeReservada(estoque.getQuantidadeReservada() - item.getValue());
        }
    }

    private void devolverEstoque(Pedido pedido) {
        Long unidadeId = pedido.getUnidade().getId();
        for (Map.Entry<Long, Integer> item : quantidades(pedido).entrySet()) {
            movimentacoes.registrar(unidadeId, item.getKey(), TipoMovimentacaoEstoque.ENTRADA, item.getValue(), null);
        }
    }

    private Map<Long, Integer> quantidades(Pedido pedido) {
        Map<Long, Integer> quantidades = new LinkedHashMap<>();
        for (ItemPedido item : pedido.getItens()) {
            quantidades.merge(item.getProduto().getId(), item.getQuantidade(), Integer::sum);
        }
        return quantidades;
    }

    private List<Reserva> reservar(Unidade unidade, List<ItemSolicitado> itens) {
        List<Reserva> reservas = new ArrayList<>();
        Map<Long, Integer> acumulado = new HashMap<>();
        for (int indice = 0; indice < itens.size(); indice++) {
            ItemSolicitado solicitado = itens.get(indice);
            Produto produto = produtos.findById(solicitado.produtoId()).orElseThrow(RecursoNaoEncontradoException::new);
            if (!produto.isAtivo()) {
                throw new RecursoInativoException("itens[" + indice + "].produtoId");
            }
            Optional<Estoque> encontrado = estoques.findByUnidadeIdAndProdutoId(unidade.getId(), produto.getId());
            int jaSolicitado = acumulado.getOrDefault(produto.getId(), 0);
            int disponivel = encontrado.map(Estoque::quantidadeDisponivel).orElse(0) - jaSolicitado;
            if (encontrado.isEmpty() || solicitado.quantidade() > disponivel) {
                throw new ReservaExcedidaException(indice, Math.max(disponivel, 0));
            }
            acumulado.put(produto.getId(), jaSolicitado + solicitado.quantidade());
            reservas.add(new Reserva(encontrado.get(), produto, solicitado.quantidade()));
        }
        return reservas;
    }

    private <T extends Enum<T>> T enumOuNulo(String valor, Class<T> tipo, String campo, List<Campo> campos) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.trim());
        } catch (IllegalArgumentException excecao) {
            campos.add(new Campo(campo, "valor inválido"));
            return null;
        }
    }

    private List<Pedido> carregar(List<Pedido> pagina) {
        if (pagina.isEmpty()) {
            return List.of();
        }
        List<Long> ids = pagina.stream().map(Pedido::getId).toList();
        Map<Long, Pedido> porId = new LinkedHashMap<>();
        for (Pedido pedido : pedidos.buscarComItens(ids)) {
            porId.putIfAbsent(pedido.getId(), pedido);
        }
        return ids.stream().map(porId::get).toList();
    }

    private ResultadoPedido resultado(Pedido pedido) {
        List<ItemDoPedido> itens = pedido.getItens().stream()
                .map(item -> new ItemDoPedido(item.getProduto().getId(), item.getQuantidade(), item.getPrecoUnitario()))
                .toList();
        return new ResultadoPedido(
                pedido.getId(),
                pedido.getUnidade().getId(),
                pedido.getCliente().getId(),
                pedido.getCanalPedido(),
                pedido.getStatus(),
                pedido.getValorTotal(),
                itens,
                pedido.getDataCriacao());
    }

    private record Reserva(Estoque estoque, Produto produto, int quantidade) {
    }
}
