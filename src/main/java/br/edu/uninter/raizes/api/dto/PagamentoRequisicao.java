package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.domain.enumeracao.ResultadoSimulado;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Simulação do retorno do gateway. O valor cobrado é o total do pedido, já com desconto de pontos.")
public record PagamentoRequisicao(
        @NotNull(message = "é obrigatório")
        @Schema(description = "Pedido em AGUARDANDO_PAGAMENTO.", example = "20")
        Long pedidoId,
        @NotNull(message = "é obrigatório")
        @Schema(description = "Resultado simulado. INDISPONIVEL não grava pagamento.", example = "APROVADO")
        ResultadoSimulado resultadoSimulado) {
}
