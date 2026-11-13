package br.edu.uninter.raizes.application.servico;

import java.math.BigDecimal;

public record ItemDoPedido(Long produtoId, int quantidade, BigDecimal precoUnitario) {
}
