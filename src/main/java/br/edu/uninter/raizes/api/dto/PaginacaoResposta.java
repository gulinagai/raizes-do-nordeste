package br.edu.uninter.raizes.api.dto;

public record PaginacaoResposta(int pagina, int limite, long totalItens, int totalPaginas) {
}
