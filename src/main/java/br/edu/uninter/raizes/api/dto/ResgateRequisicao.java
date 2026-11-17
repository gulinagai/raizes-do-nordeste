package br.edu.uninter.raizes.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ResgateRequisicao(
        @NotNull(message = "é obrigatório") Long pedidoId,
        @NotNull(message = "é obrigatório") @Min(value = 1, message = "deve ser maior que zero") Integer pontos) {
}
