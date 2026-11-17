package br.edu.uninter.raizes.application.servico;

import br.edu.uninter.raizes.domain.RegraDePontos;
import br.edu.uninter.raizes.domain.entidade.Fidelidade;
import br.edu.uninter.raizes.domain.entidade.Pedido;
import br.edu.uninter.raizes.domain.entidade.Usuario;
import br.edu.uninter.raizes.domain.enumeracao.AcaoAuditoria;
import br.edu.uninter.raizes.domain.enumeracao.Perfil;
import br.edu.uninter.raizes.domain.enumeracao.StatusPedido;
import br.edu.uninter.raizes.domain.excecao.AcessoNegadoException;
import br.edu.uninter.raizes.domain.excecao.RecursoNaoEncontradoException;
import br.edu.uninter.raizes.domain.excecao.ResgateNaoPermitidoException;
import br.edu.uninter.raizes.infrastructure.persistencia.FidelidadeRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.PedidoRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FidelidadeServico {

    private final FidelidadeRepositorio fidelidades;
    private final UsuarioRepositorio usuarios;
    private final PedidoRepositorio pedidos;
    private final AuditoriaServico auditoria;

    public FidelidadeServico(
            FidelidadeRepositorio fidelidades,
            UsuarioRepositorio usuarios,
            PedidoRepositorio pedidos,
            AuditoriaServico auditoria) {
        this.fidelidades = fidelidades;
        this.usuarios = usuarios;
        this.pedidos = pedidos;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public SituacaoFidelidade consultar(Long usuarioId) {
        return fidelidades.findByUsuarioId(usuarioId)
                .map(SituacaoFidelidade::de)
                .orElseGet(SituacaoFidelidade::ausente);
    }

    @Transactional
    public SituacaoFidelidade registrar(Long usuarioId, boolean consentimento) {
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow(RecursoNaoEncontradoException::new);
        if (usuario.getPerfil() != Perfil.CLIENTE) {
            throw new AcessoNegadoException();
        }
        Optional<Fidelidade> existente = fidelidades.findByUsuarioId(usuarioId);
        boolean haviaConsentimento = existente.map(Fidelidade::isConsentimento).orElse(false);
        Fidelidade conta;
        boolean mudou;
        if (existente.isEmpty()) {
            conta = new Fidelidade(usuario, consentimento);
            mudou = true;
        } else {
            conta = existente.get();
            mudou = conta.definirConsentimento(consentimento);
        }
        Fidelidade salva = fidelidades.saveAndFlush(conta);
        if (mudou) {
            auditoria.registrar(
                    usuarioId,
                    AcaoAuditoria.REGISTRAR_CONSENTIMENTO,
                    "fidelidade",
                    salva.getId(),
                    consentimento ? "ACEITO" : (haviaConsentimento ? "REVOGADO" : "RECUSADO"));
        }
        return SituacaoFidelidade.de(salva);
    }

    @Transactional
    public ResultadoResgate resgatar(Long usuarioId, Long pedidoId, int pontos) {
        Pedido pedido = pedidos.findById(pedidoId).orElseThrow(RecursoNaoEncontradoException::new);
        if (!pedido.getCliente().getId().equals(usuarioId)) {
            throw new AcessoNegadoException();
        }
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new ResgateNaoPermitidoException("O pedido não está aguardando pagamento.");
        }
        if (pedido.getPontosResgatados() > 0) {
            throw new ResgateNaoPermitidoException("O pedido já possui resgate de pontos.");
        }
        Fidelidade conta = fidelidades.findByUsuarioId(usuarioId)
                .filter(Fidelidade::isConsentimento)
                .orElseThrow(() -> new ResgateNaoPermitidoException(
                        "O cliente não participa do programa de fidelização."));
        if (pontos > conta.getSaldo()) {
            throw new ResgateNaoPermitidoException("Saldo de pontos insuficiente.");
        }
        BigDecimal desconto = RegraDePontos.descontoPorPontos(pontos);
        if (desconto.compareTo(pedido.getValorTotal()) > 0) {
            throw new ResgateNaoPermitidoException("O desconto não pode superar o total do pedido.");
        }
        conta.debitar(pontos);
        pedido.aplicarResgate(pontos, desconto);
        fidelidades.saveAndFlush(conta);
        pedidos.saveAndFlush(pedido);
        auditoria.registrar(
                usuarioId, AcaoAuditoria.RESGATAR_PONTOS, "pedido", pedido.getId(), String.valueOf(pontos));
        return new ResultadoResgate(pedido.getId(), pontos, desconto, pedido.getValorTotal(), conta.getSaldo());
    }

    @Transactional
    public void creditarEntrega(Pedido pedido, Long usuarioId) {
        if (pedido.isPontosCreditados()) {
            return;
        }
        int pontos = RegraDePontos.pontosPorValorPago(pedido.getValorTotal());
        Optional<Fidelidade> conta = fidelidades.findByUsuarioId(pedido.getCliente().getId());
        if (pontos > 0 && conta.isPresent() && conta.get().isConsentimento()) {
            conta.get().creditar(pontos);
            fidelidades.saveAndFlush(conta.get());
            auditoria.registrar(
                    usuarioId, AcaoAuditoria.CREDITAR_PONTOS, "pedido", pedido.getId(), String.valueOf(pontos));
        }
        pedido.registrarCredito();
    }

    @Transactional
    public void estornarResgate(Pedido pedido, Long usuarioId) {
        if (pedido.isResgateEstornado() || pedido.getPontosResgatados() == 0) {
            return;
        }
        int pontos = pedido.getPontosResgatados();
        Fidelidade conta = fidelidades.findByUsuarioId(pedido.getCliente().getId())
                .orElseThrow(RecursoNaoEncontradoException::new);
        conta.estornar(pontos);
        pedido.marcarResgateEstornado();
        fidelidades.saveAndFlush(conta);
        auditoria.registrar(
                usuarioId, AcaoAuditoria.DEVOLVER_PONTOS, "pedido", pedido.getId(), String.valueOf(pontos));
    }
}
