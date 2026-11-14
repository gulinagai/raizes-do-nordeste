package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.ResultadoListaPedidos;
import java.util.List;

public record PedidosResposta(List<PedidoResposta> itens, PaginacaoResposta paginacao) {

    public static PedidosResposta de(ResultadoListaPedidos resultado) {
        return new PedidosResposta(
                resultado.itens().stream().map(PedidoResposta::de).toList(),
                new PaginacaoResposta(
                        resultado.pagina(), resultado.limite(), resultado.totalItens(), resultado.totalPaginas()));
    }
}
