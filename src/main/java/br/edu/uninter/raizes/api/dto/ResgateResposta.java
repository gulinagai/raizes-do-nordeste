package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.ResultadoResgate;
import java.math.BigDecimal;

public record ResgateResposta(Long pedidoId, int pontos, BigDecimal desconto, BigDecimal total, int saldo) {

    public static ResgateResposta de(ResultadoResgate resultado) {
        return new ResgateResposta(
                resultado.pedidoId(), resultado.pontos(), resultado.desconto(), resultado.total(), resultado.saldo());
    }
}
