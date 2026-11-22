package br.edu.uninter.raizes.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Item solicitado no pedido.")
public record ItemPedidoRequisicao(
        @NotNull(message = "é obrigatório")
        @Schema(description = "Produto ativo com estoque na unidade.", example = "1")
        Long produtoId,
        @NotNull(message = "é obrigatória")
        @Min(value = 1, message = "deve ser maior que zero")
        @Schema(description = "Quantidade. Reserva o estoque disponível e não reduz o estoque físico.", example = "1")
        Integer quantidade) {
}
