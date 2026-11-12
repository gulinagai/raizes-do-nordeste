package br.edu.uninter.raizes.api.controller;

import br.edu.uninter.raizes.api.dto.MovimentacaoEstoqueRequisicao;
import br.edu.uninter.raizes.api.dto.MovimentacaoEstoqueResposta;
import br.edu.uninter.raizes.application.servico.MovimentacaoEstoqueServico;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/estoque/movimentacoes")
public class MovimentacaoEstoqueController {

    private final MovimentacaoEstoqueServico movimentacaoEstoqueServico;

    public MovimentacaoEstoqueController(MovimentacaoEstoqueServico movimentacaoEstoqueServico) {
        this.movimentacaoEstoqueServico = movimentacaoEstoqueServico;
    }

    @PostMapping
    public ResponseEntity<MovimentacaoEstoqueResposta> registrar(
            @Valid @RequestBody MovimentacaoEstoqueRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MovimentacaoEstoqueResposta.de(movimentacaoEstoqueServico.registrar(
                        requisicao.unidadeId(),
                        requisicao.produtoId(),
                        requisicao.tipo(),
                        requisicao.quantidade(),
                        requisicao.observacao())));
    }
}
