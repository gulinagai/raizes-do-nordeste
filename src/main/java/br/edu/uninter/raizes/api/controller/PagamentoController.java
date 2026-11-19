package br.edu.uninter.raizes.api.controller;

import br.edu.uninter.raizes.api.dto.PagamentoRequisicao;
import br.edu.uninter.raizes.api.dto.PagamentoResposta;
import br.edu.uninter.raizes.api.erro.ErroResposta;
import br.edu.uninter.raizes.application.servico.PagamentoServico;
import br.edu.uninter.raizes.application.servico.ResultadoPagamento;
import br.edu.uninter.raizes.domain.excecao.GatewayPagamentoIndisponivelException;
import br.edu.uninter.raizes.infrastructure.seguranca.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagamentos")
@Tag(name = "Pagamentos", description = "Simulação do gateway de pagamento.")
public class PagamentoController{

    private final PagamentoServico pagamentoServico;

    public PagamentoController(PagamentoServico pagamentoServico) {
        this.pagamentoServico = pagamentoServico;
    }

    @Operation(
            summary = "Solicitar pagamento",
            description = """
                    Perfis ADMIN, GERENTE, ATENDENTE e CLIENTE. O cliente só paga o próprio pedido. \
                    O valor é o total do pedido. APROVADO baixa a reserva e o estoque físico e deixa o pedido EM_PREPARO. \
                    RECUSADO libera só a reserva, devolve pontos já resgatados e deixa o pedido PAGAMENTO_RECUSADO. \
                    INDISPONIVEL não grava pagamento nem altera saldo.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pagamento aprovado ou recusado, já registrado."),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido. O código do corpo é NAO_AUTENTICADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Perfil sem permissão ou cliente pagando pedido de outra pessoa. O código do corpo é ACESSO_NEGADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Pedido não encontrado. O código do corpo é NAO_ENCONTRADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Pedido fora de AGUARDANDO_PAGAMENTO. O código do corpo é PAGAMENTO_NAO_PERMITIDO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "Campo ausente, resultado inválido ou JSON inválido. O código do corpo é VALIDACAO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "503",
                    description = "Gateway simulado indisponível. O código do corpo é GATEWAY_PAGAMENTO_INDISPONIVEL. Nada é gravado.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<PagamentoResposta> solicitar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody PagamentoRequisicao requisicao) {
        ResultadoPagamento resultado = pagamentoServico.solicitar(
                requisicao.pedidoId(), requisicao.resultadoSimulado(), autenticado.id(), autenticado.perfil());
        if (resultado.gatewayIndisponivel()) {
            throw new GatewayPagamentoIndisponivelException();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(PagamentoResposta.de(resultado));
    }
}
