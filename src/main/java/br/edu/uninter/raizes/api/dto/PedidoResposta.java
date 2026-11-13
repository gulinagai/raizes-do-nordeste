package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.ResultadoPedido;
import br.edu.uninter.raizes.domain.enumeracao.CanalPedido;
import br.edu.uninter.raizes.domain.enumeracao.StatusPedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PedidoResposta(
        Long id,
        Long unidadeId,
        Long clienteId,
        CanalPedido canalPedido,
        StatusPedido status,
        BigDecimal total,
        List<ItemPedidoResposta> itens,
        Instant dataCriacao) {

    public static PedidoResposta de(ResultadoPedido resultado) {
        return new PedidoResposta(
                resultado.id(),
                resultado.unidadeId(),
                resultado.clienteId(),
                resultado.canalPedido(),
                resultado.status(),
                resultado.total(),
                resultado.itens().stream().map(ItemPedidoResposta::de).toList(),
                resultado.dataCriacao());
    }
}
