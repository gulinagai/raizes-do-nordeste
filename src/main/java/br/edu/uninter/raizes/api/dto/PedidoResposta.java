package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.ResultadoPedido;
import br.edu.uninter.raizes.domain.enumeracao.CanalPedido;
import br.edu.uninter.raizes.domain.enumeracao.StatusPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Pedido com o total já considerado o desconto de pontos, quando houver resgate.")
public record PedidoResposta(
        @Schema(description = "Identificador do pedido.", example = "20")
        Long id,
        @Schema(description = "Unidade do pedido.", example = "1")
        Long unidadeId,
        @Schema(description = "Cliente do pedido.", example = "5")
        Long clienteId,
        @Schema(description = "Canal de origem.", example = "APP")
        CanalPedido canalPedido,
        @Schema(description = "Situação atual.", example = "AGUARDANDO_PAGAMENTO")
        StatusPedido status,
        @Schema(description = "Valor a pagar, depois do desconto.", example = "34.90")
        BigDecimal total,
        @Schema(description = "Desconto obtido com pontos. Zero quando não houve resgate.", example = "0.00")
        BigDecimal desconto,
        @Schema(description = "Pontos usados no resgate. Zero quando não houve resgate.", example = "0")
        int pontosResgatados,
        @Schema(description = "Itens com o preço unitário gravado na criação.")
        List<ItemPedidoResposta> itens,
        @Schema(description = "Momento da criação.", example = "2026-11-16T18:00:00Z")
        Instant dataCriacao) {

    public static PedidoResposta de(ResultadoPedido resultado) {
        return new PedidoResposta(
                resultado.id(),
                resultado.unidadeId(),
                resultado.clienteId(),
                resultado.canalPedido(),
                resultado.status(),
                resultado.total(),
                resultado.desconto(),
                resultado.pontosResgatados(),
                resultado.itens().stream().map(ItemPedidoResposta::de).toList(),
                resultado.dataCriacao());
    }
}
