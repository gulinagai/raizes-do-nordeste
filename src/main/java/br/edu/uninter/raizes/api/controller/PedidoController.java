package br.edu.uninter.raizes.api.controller;

import br.edu.uninter.raizes.api.dto.AtualizacaoStatusRequisicao;
import br.edu.uninter.raizes.api.dto.PedidoRequisicao;
import br.edu.uninter.raizes.api.dto.PedidoResposta;
import br.edu.uninter.raizes.api.dto.PedidosResposta;
import br.edu.uninter.raizes.application.servico.AuditoriaServico;
import br.edu.uninter.raizes.application.servico.ItemSolicitado;
import br.edu.uninter.raizes.application.servico.PedidoServico;
import br.edu.uninter.raizes.domain.enumeracao.AcaoAuditoria;
import br.edu.uninter.raizes.infrastructure.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoServico pedidoServico;
    private final AuditoriaServico auditoria;

    public PedidoController(PedidoServico pedidoServico, AuditoriaServico auditoria) {
        this.pedidoServico = pedidoServico;
        this.auditoria = auditoria;
    }

    @GetMapping
    public PedidosResposta listar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @RequestParam(name = "canalPedido", required = false) String canalPedido,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        PedidosResposta resposta = PedidosResposta.de(pedidoServico.listar(
                autenticado.id(), autenticado.perfil(), canalPedido, status, page, limit));
        auditoria.registrar(autenticado.id(), AcaoAuditoria.CONSULTAR_PEDIDOS, "pedido", null, "CONSULTA");
        return resposta;
    }

    @PatchMapping("/{id}/cancelamento")
    public PedidoResposta cancelar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return PedidoResposta.de(pedidoServico.cancelar(id, autenticado.id(), autenticado.perfil()));
    }

    @PatchMapping("/{id}/status")
    public PedidoResposta atualizarStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody AtualizacaoStatusRequisicao requisicao) {
        return PedidoResposta.de(
                pedidoServico.atualizarStatus(id, requisicao.status(), autenticado.id(), autenticado.perfil()));
    }

    @PostMapping
    public ResponseEntity<PedidoResposta> criar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado, @Valid @RequestBody PedidoRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResposta.de(pedidoServico.criar(
                autenticado.id(),
                autenticado.perfil(),
                requisicao.unidadeId(),
                requisicao.clienteId(),
                requisicao.canalPedido(),
                requisicao.itens().stream()
                        .map(item -> new ItemSolicitado(item.produtoId(), item.quantidade()))
                        .toList())));
    }
}
