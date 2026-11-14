package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.ItemDoPedido;
import java.math.BigDecimal;

public record ItemPedidoResposta(Long produtoId, int quantidade, BigDecimal precoUnitario) {

    static ItemPedidoResposta de(ItemDoPedido item) {
        return new ItemPedidoResposta(item.produtoId(), item.quantidade(), item.precoUnitario());
    }
}
