package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.ItemDoPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Item gravado no pedido.")
public record ItemPedidoResposta(
        @Schema(description = "Identificador do produto.", example = "1")
        Long produtoId,
        @Schema(description = "Quantidade do item.", example = "1")
        int quantidade,
        @Schema(description = "Preço unitário no momento do pedido.", example = "34.90")
        BigDecimal precoUnitario) {

    static ItemPedidoResposta de(ItemDoPedido item) {
        return new ItemPedidoResposta(item.produtoId(), item.quantidade(), item.precoUnitario());
    }
}
