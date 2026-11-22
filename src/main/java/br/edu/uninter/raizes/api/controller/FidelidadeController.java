package br.edu.uninter.raizes.api.controller;

import br.edu.uninter.raizes.api.dto.ConsentimentoRequisicao;
import br.edu.uninter.raizes.api.dto.FidelidadeResposta;
import br.edu.uninter.raizes.api.dto.ResgateRequisicao;
import br.edu.uninter.raizes.api.dto.ResgateResposta;
import br.edu.uninter.raizes.api.erro.ErroResposta;
import br.edu.uninter.raizes.application.servico.FidelidadeServico;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fidelidade")
@Tag(name = "Fidelidade", description = "Consentimento, saldo e resgate de pontos do cliente.")
public class FidelidadeController {

    private final FidelidadeServico fidelidadeServico;

    public FidelidadeController(FidelidadeServico fidelidadeServico) {
        this.fidelidadeServico = fidelidadeServico;
    }

    @Operation(
            summary = "Consultar fidelidade",
            description = """
                    Perfil CLIENTE. Devolve o consentimento e o saldo do próprio usuário. \
                    Sem registro prévio, responde saldo zero e consentimento falso, sem criar conta.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Situação do programa."),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido. O código do corpo é NAO_AUTENTICADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Perfil diferente de CLIENTE. O código do corpo é ACESSO_NEGADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public FidelidadeResposta consultar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return FidelidadeResposta.de(fidelidadeServico.consultar(autenticado.id()));
    }

    @Operation(
            summary = "Registrar consentimento",
            description = """
                    Perfil CLIENTE. Aceita, recusa ou revoga a participação no programa de pontos. \
                    Revogar não apaga o saldo. Um novo aceite reabre o programa com o saldo existente.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consentimento registrado."),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido. O código do corpo é NAO_AUTENTICADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Perfil diferente de CLIENTE. O código do corpo é ACESSO_NEGADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "Campo consentimento ausente ou JSON inválido. O código do corpo é VALIDACAO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping
    public ResponseEntity<FidelidadeResposta> registrar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody ConsentimentoRequisicao requisicao) {
        return ResponseEntity.ok(FidelidadeResposta.de(
                fidelidadeServico.registrar(autenticado.id(), requisicao.consentimento())));
    }

    @Operation(
            summary = "Resgatar pontos",
            description = """
                    Perfil CLIENTE, com consentimento ativo, no próprio pedido em AGUARDANDO_PAGAMENTO. \
                    Cem pontos viram R$ 1,00 de desconto. O total não fica negativo. \
                    Cada pedido aceita um resgate. O débito ocorre aqui, antes do pagamento.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Desconto aplicado e saldo atualizado."),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido. O código do corpo é NAO_AUTENTICADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Perfil diferente de CLIENTE ou pedido de outro cliente. O código do corpo é ACESSO_NEGADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Pedido não encontrado. O código do corpo é NAO_ENCONTRADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                            Sem consentimento, saldo insuficiente, pedido fora de pagamento, \
                            resgate já feito ou desconto maior que o total. O código do corpo é RESGATE_NAO_PERMITIDO.""",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "Campo ausente, pontos menores que 1 ou JSON inválido. O código do corpo é VALIDACAO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/resgates")
    public ResponseEntity<ResgateResposta> resgatar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody ResgateRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ResgateResposta.de(
                fidelidadeServico.resgatar(autenticado.id(), requisicao.pedidoId(), requisicao.pontos())));
    }
}
