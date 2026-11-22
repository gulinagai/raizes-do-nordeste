package br.edu.uninter.raizes.api.controller;

import br.edu.uninter.raizes.api.dto.MovimentacaoEstoqueRequisicao;
import br.edu.uninter.raizes.api.dto.MovimentacaoEstoqueResposta;
import br.edu.uninter.raizes.api.erro.ErroResposta;
import br.edu.uninter.raizes.application.servico.MovimentacaoEstoqueServico;
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
@RequestMapping("/estoque/movimentacoes")
@Tag(name = "Estoque", description = "Entrada e saída manual de estoque por unidade.")
public class MovimentacaoEstoqueController {

    private final MovimentacaoEstoqueServico movimentacaoEstoqueServico;

    public MovimentacaoEstoqueController(MovimentacaoEstoqueServico movimentacaoEstoqueServico) {
        this.movimentacaoEstoqueServico = movimentacaoEstoqueServico;
    }

    @Operation(
            summary = "Registrar movimentação",
            description = """
                    Perfis ADMIN e GERENTE. ENTRADA cria o saldo quando ele não existe. \
                    SAIDA só retira quantidade disponível e não mexe na quantidade reservada.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Movimentação registrada."),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido. O código do corpo é NAO_AUTENTICADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Perfil diferente de ADMIN e GERENTE. O código do corpo é ACESSO_NEGADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Unidade ou produto não encontrado. O código do corpo é NAO_ENCONTRADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Saída acima da quantidade disponível. O código do corpo é ESTOQUE_INSUFICIENTE.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "Campo obrigatório ausente, quantidade menor que 1 ou JSON inválido. O código do corpo é VALIDACAO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<MovimentacaoEstoqueResposta> registrar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody MovimentacaoEstoqueRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MovimentacaoEstoqueResposta.de(movimentacaoEstoqueServico.registrar(
                        requisicao.unidadeId(),
                        requisicao.produtoId(),
                        requisicao.tipo(),
                        requisicao.quantidade(),
                        requisicao.observacao(),
                        autenticado.id())));
    }
}
