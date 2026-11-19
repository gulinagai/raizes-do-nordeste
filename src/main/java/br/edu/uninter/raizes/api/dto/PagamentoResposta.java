package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.ResultadoPagamento;
import br.edu.uninter.raizes.domain.enumeracao.StatusPagamento;
import br.edu.uninter.raizes.domain.enumeracao.StatusPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Pagamento registrado. Aprovado leva o pedido a EM_PREPARO. Recusado leva a PAGAMENTO_RECUSADO.")
public record PagamentoResposta(
        @Schema(description = "Identificador do pagamento.", example = "8")
        Long id,
        @Schema(description = "Pedido pago.", example = "20")
        Long pedidoId,
        @Schema(description = "Resultado gravado.", example = "APROVADO")
        StatusPagamento status,
        @Schema(description = "Valor cobrado.", example = "34.90")
        BigDecimal valor,
        @Schema(description = "Status do pedido depois do pagamento.", example = "EM_PREPARO")
        StatusPedido statusPedido,
        @Schema(description = "Momento do pagamento.", example = "2026-11-16T18:05:00Z")
        Instant dataHora) {

    public static PagamentoResposta de(ResultadoPagamento resultado) {
        return new PagamentoResposta(
                resultado.id(),
                resultado.pedidoId(),
                resultado.status(),
                resultado.valor(),
                resultado.statusPedido(),
                resultado.dataHora());
    }
}
