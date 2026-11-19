package br.edu.uninter.raizes.api.erro;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Campo que impediu a operação.")
public record DetalheErro(
        @Schema(description = "Nome do campo.", example = "email")
        String field,
        @Schema(description = "Motivo da rejeição.", example = "deve ser um e-mail válido")
        String issue) {
}
