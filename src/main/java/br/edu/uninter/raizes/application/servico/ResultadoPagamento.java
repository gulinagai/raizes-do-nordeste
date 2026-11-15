package br.edu.uninter.raizes.application.servico;

import br.edu.uninter.raizes.domain.enumeracao.StatusPagamento;
import br.edu.uninter.raizes.domain.enumeracao.StatusPedido;
import java.math.BigDecimal;
import java.time.Instant;

public record ResultadoPagamento(
        boolean gatewayIndisponivel,
        Long id,
        Long pedidoId,
        StatusPagamento status,
        BigDecimal valor,
        StatusPedido statusPedido,
        Instant dataHora) {

    public static ResultadoPagamento indisponivel() {
        return new ResultadoPagamento(true, null, null, null, null, null, null);
    }
}
