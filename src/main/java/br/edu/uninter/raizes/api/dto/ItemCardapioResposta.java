package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.ItemDeCardapio;
import java.math.BigDecimal;

public record ItemCardapioResposta(
        Long produtoId,
        String nome,
        String descricao,
        BigDecimal preco,
        int quantidadeDisponivel,
        boolean disponivel) {

    static ItemCardapioResposta de(ItemDeCardapio item) {
        return new ItemCardapioResposta(
                item.produtoId(),
                item.nome(),
                item.descricao(),
                item.preco(),
                item.quantidadeDisponivel(),
                item.disponivel());
    }
}
