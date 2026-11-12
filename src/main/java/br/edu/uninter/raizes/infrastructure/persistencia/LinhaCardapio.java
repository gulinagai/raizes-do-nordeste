package br.edu.uninter.raizes.infrastructure.persistencia;

import java.math.BigDecimal;

public record LinhaCardapio(
        Long produtoId,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer quantidadeDisponivel) {
}
