package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.ResultadoCardapio;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Cardápio de uma unidade, com os produtos e a página consultada.")
public record CardapioResposta(
        @Schema(description = "Unidade consultada.")
        UnidadeCardapioResposta unidade,
        @Schema(description = "Produtos da página.")
        List<ItemCardapioResposta> itens,
        @Schema(description = "Dados da página.")
        PaginacaoResposta paginacao) {

    public static CardapioResposta de(ResultadoCardapio resultado) {
        return new CardapioResposta(
                new UnidadeCardapioResposta(resultado.unidadeId(), resultado.unidadeNome()),
                resultado.itens().stream().map(ItemCardapioResposta::de).toList(),
                new PaginacaoResposta(
                        resultado.pagina(), resultado.limite(), resultado.totalItens(), resultado.totalPaginas()));
    }

    @Schema(description = "Identificação da unidade do cardápio.")
    public record UnidadeCardapioResposta(
            @Schema(description = "Identificador da unidade.", example = "1")
            Long id,
            @Schema(description = "Nome da unidade.", example = "Raízes Matriz Recife")
            String nome) {
    }
}
