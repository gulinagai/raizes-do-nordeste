package br.edu.uninter.raizes.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginação usada nas consultas. A página começa em 1 e o limite vai de 1 a 50.")
public record PaginacaoResposta(
        @Schema(description = "Página devolvida.", example = "1")
        int pagina,
        @Schema(description = "Quantidade pedida por página.", example = "10")
        int limite,
        @Schema(description = "Total de registros que atendem ao filtro.", example = "1")
        long totalItens,
        @Schema(description = "Total de páginas. Zero quando não há registros.", example = "1")
        int totalPaginas) {
}
