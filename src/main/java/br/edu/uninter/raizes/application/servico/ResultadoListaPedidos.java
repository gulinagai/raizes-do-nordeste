package br.edu.uninter.raizes.application.servico;


import java.util.List;

public record ResultadoListaPedidos(
        List<ResultadoPedido> itens,
        int pagina,
        int limite,
        long totalItens,
        int totalPaginas) {
}
