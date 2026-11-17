package br.edu.uninter.raizes.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class RegraDePontos {

    public static final int PONTOS_POR_REAL_PAGO = 1;
    public static final int PONTOS_POR_REAL_DE_DESCONTO = 100;

    private RegraDePontos() {
    }

    public static int pontosPorValorPago(BigDecimal valorPago) {
        return valorPago.setScale(0, RoundingMode.DOWN).intValue() * PONTOS_POR_REAL_PAGO;
    }

    public static BigDecimal descontoPorPontos(int pontos) {
        return BigDecimal.valueOf(pontos)
                .divide(BigDecimal.valueOf(PONTOS_POR_REAL_DE_DESCONTO), 2, RoundingMode.DOWN);
    }
}
