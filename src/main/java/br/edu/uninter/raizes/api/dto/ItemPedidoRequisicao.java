package br.edu.uninter.raizes.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemPedidoRequisicao(
        @NotNull(message = "é obrigatório") Long produtoId,
        @NotNull(message = "é obrigatória") @Min(value = 1, message = "deve ser maior que zero") Integer quantidade) {
}
