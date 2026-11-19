package br.edu.uninter.raizes.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Resgate de pontos do próprio cliente em um pedido que ainda aguarda pagamento. Cem pontos valem R$ 1,00.")
public record ResgateRequisicao(
        @NotNull(message = "é obrigatório")
        @Schema(description = "Pedido do próprio cliente em AGUARDANDO_PAGAMENTO.", example = "21")
        Long pedidoId,
        @NotNull(message = "é obrigatório")
        @Min(value = 1, message = "deve ser maior que zero")
        @Schema(description = "Pontos a usar. O desconto não pode superar o total do pedido.", example = "100")
        Integer pontos) {
}
