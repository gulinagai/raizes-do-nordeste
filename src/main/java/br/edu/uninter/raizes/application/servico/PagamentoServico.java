package br.edu.uninter.raizes.application.servico;

import br.edu.uninter.raizes.domain.entidade.Estoque;
import br.edu.uninter.raizes.domain.entidade.ItemPedido;
import br.edu.uninter.raizes.domain.entidade.MovimentacaoEstoque;
import br.edu.uninter.raizes.domain.entidade.Pagamento;
import br.edu.uninter.raizes.domain.entidade.Pedido;
import br.edu.uninter.raizes.domain.enumeracao.ResultadoSimulado;
import br.edu.uninter.raizes.domain.enumeracao.StatusPagamento;
import br.edu.uninter.raizes.domain.enumeracao.StatusPedido;
import br.edu.uninter.raizes.domain.enumeracao.TipoMovimentacaoEstoque;
import br.edu.uninter.raizes.domain.excecao.EstoqueInsuficienteException;
import br.edu.uninter.raizes.domain.excecao.PagamentoNaoPermitidoException;
import br.edu.uninter.raizes.domain.excecao.RecursoNaoEncontradoException;
import br.edu.uninter.raizes.infrastructure.pagamento.RetornoSimulado;
import br.edu.uninter.raizes.infrastructure.pagamento.SimuladorDePagamento;
import br.edu.uninter.raizes.infrastructure.persistencia.EstoqueRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.MovimentacaoEstoqueRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.PagamentoRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.PedidoRepositorio;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagamentoServico {

    private final PedidoRepositorio pedidos;
    private final EstoqueRepositorio estoques;
    private final MovimentacaoEstoqueRepositorio movimentacoes;
    private final PagamentoRepositorio pagamentos;
    private final SimuladorDePagamento simulador;

    public PagamentoServico(
            PedidoRepositorio pedidos,
            EstoqueRepositorio estoques,
            MovimentacaoEstoqueRepositorio movimentacoes,
            PagamentoRepositorio pagamentos,
            SimuladorDePagamento simulador) {
        this.pedidos = pedidos;
        this.estoques = estoques;
        this.movimentacoes = movimentacoes;
        this.pagamentos = pagamentos;
        this.simulador = simulador;
    }

    @Transactional
    public ResultadoPagamento solicitar(Long pedidoId, ResultadoSimulado resultadoSimulado) {
        Pedido pedido = pedidos.findById(pedidoId).orElseThrow(RecursoNaoEncontradoException::new);
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new PagamentoNaoPermitidoException();
        }
        RetornoSimulado retorno = simulador.simular(resultadoSimulado);
        if (retorno.gatewayIndisponivel()) {
            return ResultadoPagamento.indisponivel();
        }
        boolean aprovado = retorno.status() == StatusPagamento.APROVADO;
        confirmarEstoque(pedido, aprovado);
        pedido.definirStatus(aprovado ? StatusPedido.EM_PREPARO : StatusPedido.PAGAMENTO_RECUSADO);
        Pagamento pagamento = pagamentos.saveAndFlush(new Pagamento(pedido, retorno.status(), pedido.getValorTotal()));
        return new ResultadoPagamento(
                false,
                pagamento.getId(),
                pedido.getId(),
                pagamento.getStatus(),
                pagamento.getValor(),
                pedido.getStatus(),
                pagamento.getDataHora());
    }

    private void confirmarEstoque(Pedido pedido, boolean aprovado) {
        Map<Long, Integer> quantidades = new LinkedHashMap<>();
        for (ItemPedido item : pedido.getItens()) {
            quantidades.merge(item.getProduto().getId(), item.getQuantidade(), Integer::sum);
        }
        for (Map.Entry<Long, Integer> item : quantidades.entrySet()) {
            Estoque estoque = estoques.findByUnidadeIdAndProdutoId(pedido.getUnidade().getId(), item.getKey())
                    .orElseThrow(() -> new EstoqueInsuficienteException(0));
            int quantidade = item.getValue();
            if (estoque.getQuantidadeReservada() < quantidade || (aprovado && estoque.getQuantidadeTotal() < quantidade)) {
                throw new EstoqueInsuficienteException(estoque.getQuantidadeReservada());
            }
            estoque.setQuantidadeReservada(estoque.getQuantidadeReservada() - quantidade);
            if (aprovado) {
                estoque.setQuantidadeTotal(estoque.getQuantidadeTotal() - quantidade);
                movimentacoes.save(new MovimentacaoEstoque(estoque, TipoMovimentacaoEstoque.SAIDA, quantidade, null));
            }
        }
    }
}
