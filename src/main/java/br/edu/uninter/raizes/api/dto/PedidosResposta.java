package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.ResultadoListaPedidos;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Página de pedidos. O perfil CLIENTE recebe somente os próprios pedidos.")
public record PedidosResposta(
        @Schema(description = "Pedidos da página, do mais recente para o mais antigo.")
        List<PedidoResposta> itens,
        @Schema(description = "Dados da página.")
        PaginacaoResposta paginacao) {

    public static PedidosResposta de(ResultadoListaPedidos resultado) {
        return new PedidosResposta(
                resultado.itens().stream().map(PedidoResposta::de).toList(),
                new PaginacaoResposta(
                        resultado.pagina(), resultado.limite(), resultado.totalItens(), resultado.totalPaginas()));
    }
}
