package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.ResultadoCardapio;
import java.util.List;

public record CardapioResposta(
        UnidadeCardapioResposta unidade,
        List<ItemCardapioResposta> itens,
        PaginacaoResposta paginacao) {

    public static CardapioResposta de(ResultadoCardapio resultado) {
        return new CardapioResposta(
                new UnidadeCardapioResposta(resultado.unidadeId(), resultado.unidadeNome()),
                resultado.itens().stream().map(ItemCardapioResposta::de).toList(),
                new PaginacaoResposta(
                        resultado.pagina(), resultado.limite(), resultado.totalItens(), resultado.totalPaginas()));
    }

    public record UnidadeCardapioResposta(Long id, String nome) {
    }
}
