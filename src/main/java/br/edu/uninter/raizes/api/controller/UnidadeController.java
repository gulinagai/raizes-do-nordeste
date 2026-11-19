package br.edu.uninter.raizes.api.controller;

import br.edu.uninter.raizes.api.dto.CardapioResposta;
import br.edu.uninter.raizes.api.erro.ErroResposta;
import br.edu.uninter.raizes.application.servico.CardapioServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/unidades")
@Tag(name = "Unidades", description = "Consulta do cardápio por unidade.")
public class UnidadeController {

    private final CardapioServico cardapioServico;

    public UnidadeController(CardapioServico cardapioServico) {
        this.cardapioServico = cardapioServico;
    }

    @Operation(
            summary = "Consultar cardápio",
            description = """
                    Lista os produtos da unidade, com preço e quantidade disponível. \
                    Qualquer perfil autenticado pode consultar. Unidade inativa continua visível.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página do cardápio."),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido. O código do corpo é NAO_AUTENTICADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Unidade não encontrada. O código do corpo é NAO_ENCONTRADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "Página menor que 1, limite fora de 1 a 50 ou identificador inválido. O código do corpo é VALIDACAO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}/cardapio")
    public CardapioResposta cardapio(
            @Parameter(description = "Identificador da unidade.", example = "1") @PathVariable Long id,
            @Parameter(description = "Página, a partir de 1.", example = "1")
            @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "Itens por página, de 1 a 50.", example = "10")
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return CardapioResposta.de(cardapioServico.consultar(id, page, limit));
    }
}
