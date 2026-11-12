package br.edu.uninter.raizes.application.servico;

import java.math.BigDecimal;

public record ItemDeCardapio(
        Long produtoId,
        String nome,
        String descricao,
        BigDecimal preco,
        int quantidadeDisponivel,
        boolean disponivel) {
}
