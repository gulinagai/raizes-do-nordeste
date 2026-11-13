package br.edu.uninter.raizes.api.controller;

import br.edu.uninter.raizes.api.dto.PedidoRequisicao;
import br.edu.uninter.raizes.api.dto.PedidoResposta;
import br.edu.uninter.raizes.application.servico.ItemSolicitado;
import br.edu.uninter.raizes.application.servico.PedidoServico;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoServico pedidoServico;

    public PedidoController(PedidoServico pedidoServico) {
        this.pedidoServico = pedidoServico;
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
