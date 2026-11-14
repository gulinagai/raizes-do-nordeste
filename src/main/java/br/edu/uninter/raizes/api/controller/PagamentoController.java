package br.edu.uninter.raizes.api.controller;

import br.edu.uninter.raizes.api.dto.PagamentoRequisicao;
import br.edu.uninter.raizes.api.dto.PagamentoResposta;
import br.edu.uninter.raizes.application.servico.PagamentoServico;
import br.edu.uninter.raizes.application.servico.ResultadoPagamento;
import br.edu.uninter.raizes.domain.excecao.GatewayPagamentoIndisponivelException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagamentos")
public class PagamentoController{

    private final PagamentoServico pagamentoServico;

    public PagamentoController(PagamentoServico pagamentoServico) {
        this.pagamentoServico = pagamentoServico;
    }

    @PostMapping
    public ResponseEntity<PagamentoResposta> solicitar(@Valid @RequestBody PagamentoRequisicao requisicao) {
        ResultadoPagamento resultado = pagamentoServico.solicitar(requisicao.pedidoId(), requisicao.resultadoSimulado());
        if (resultado.gatewayIndisponivel()) {
            throw new GatewayPagamentoIndisponivelException();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(PagamentoResposta.de(resultado));
    }
}
