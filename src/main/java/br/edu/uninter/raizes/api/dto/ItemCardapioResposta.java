package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.ItemDeCardapio;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Produto disponível para venda em uma unidade.")
public record ItemCardapioResposta(
        @Schema(description = "Identificador do produto.", example = "1")
        Long produtoId,
        @Schema(description = "Nome do produto.", example = "Baião de dois")
        String nome,
        @Schema(description = "Descrição do produto.", example = "Arroz, feijão de corda, queijo coalho e carne de sol.")
        String descricao,
        @Schema(description = "Preço unitário.", example = "34.90")
        BigDecimal preco,
        @Schema(description = "Quantidade ainda não reservada.", example = "40")
        int quantidadeDisponivel,
        @Schema(description = "Verdadeiro quando há quantidade disponível.", example = "true")
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
