package br.edu.uninter.raizes.api.controller;

import br.edu.uninter.raizes.api.dto.PedidoRequisicao;
import br.edu.uninter.raizes.api.dto.PedidoResposta;
import br.edu.uninter.raizes.api.dto.PedidosResposta;
import br.edu.uninter.raizes.application.servico.ItemSolicitado;
import br.edu.uninter.raizes.application.servico.PedidoServico;
import br.edu.uninter.raizes.infrastructure.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoServico pedidoServico;

    public PedidoController(PedidoServico pedidoServico) {
        this.pedidoServico = pedidoServico;
    }

    @GetMapping
    public PedidosResposta listar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @RequestParam(name = "canalPedido", required = false) String canalPedido,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return PedidosResposta.de(pedidoServico.listar(
                autenticado.id(), autenticado.perfil(), canalPedido, status, page, limit));
    }

    @PostMapping
    public ResponseEntity<PedidoResposta> criar(@Valid @RequestBody PedidoRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResposta.de(pedidoServico.criar(
                requisicao.unidadeId(),
                requisicao.clienteId(),
                requisicao.canalPedido(),
                requisicao.itens().stream()
                        .map(item -> new ItemSolicitado(item.produtoId(), item.quantidade()))
                        .toList())));
    }
}
