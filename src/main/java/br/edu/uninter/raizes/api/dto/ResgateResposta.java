package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.ResultadoResgate;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Desconto aplicado ao pedido e saldo restante.")
public record ResgateResposta(
        @Schema(description = "Pedido que recebeu o desconto.", example = "21")
        Long pedidoId,
        @Schema(description = "Pontos debitados.", example = "100")
        int pontos,
        @Schema(description = "Valor descontado do pedido.", example = "1.00")
        BigDecimal desconto,
        @Schema(description = "Total do pedido depois do desconto.", example = "33.90")
        BigDecimal total,
        @Schema(description = "Saldo de pontos depois do resgate.", example = "4")
        int saldo) {

    public static ResgateResposta de(ResultadoResgate resultado) {
        return new ResgateResposta(
                resultado.pedidoId(), resultado.pontos(), resultado.desconto(), resultado.total(), resultado.saldo());
    }
}
