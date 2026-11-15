package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.ResultadoPagamento;
import br.edu.uninter.raizes.domain.enumeracao.StatusPagamento;
import br.edu.uninter.raizes.domain.enumeracao.StatusPedido;
import java.math.BigDecimal;
import java.time.Instant;

public record PagamentoResposta(
        Long id,
        Long pedidoId,
        StatusPagamento status,
        BigDecimal valor,
        StatusPedido statusPedido,
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
