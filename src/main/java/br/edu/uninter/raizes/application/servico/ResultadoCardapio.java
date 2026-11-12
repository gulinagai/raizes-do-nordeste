package br.edu.uninter.raizes.application.servico;

import java.util.List;

public record ResultadoCardapio(
        Long unidadeId,
        String unidadeNome,
        List<ItemDeCardapio> itens,
        int pagina,
        int limite,
        long totalItens,
        int totalPaginas) {
}
