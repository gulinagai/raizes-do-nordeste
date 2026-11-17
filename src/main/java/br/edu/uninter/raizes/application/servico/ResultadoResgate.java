package br.edu.uninter.raizes.application.servico;

import java.math.BigDecimal;

public record ResultadoResgate(Long pedidoId, int pontos, BigDecimal desconto, BigDecimal total, int saldo) {
}
