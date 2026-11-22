package br.edu.uninter.raizes.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Decisão do cliente sobre participar do programa de pontos. Não autoriza outro uso de dados pessoais.")
public record ConsentimentoRequisicao(
        @NotNull(message = "é obrigatório")
        @Schema(description = "Verdadeiro aceita o programa. Falso revoga ou recusa.", example = "true")
        Boolean consentimento) {
}
