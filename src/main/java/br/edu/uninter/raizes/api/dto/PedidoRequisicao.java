package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.domain.enumeracao.CanalPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Pedido a criar. O preço vem do produto e a forma de pagamento, se enviada, é ignorada.")
public record PedidoRequisicao(
        @NotNull(message = "é obrigatório")
        @Schema(description = "Unidade que vai atender o pedido. Precisa estar ativa.", example = "1")
        Long unidadeId,
        @NotNull(message = "é obrigatório")
        @Schema(description = "Cliente do pedido. O perfil CLIENTE só pode informar o próprio identificador.", example = "5")
        Long clienteId,
        @NotNull(message = "é obrigatório")
        @Schema(description = "Canal de origem.", example = "APP")
        CanalPedido canalPedido,
        @NotNull(message = "é obrigatório")
        @NotEmpty(message = "deve conter ao menos um item")
        @Valid
        @Schema(description = "Itens do pedido. O mesmo produto informado mais de uma vez tem as quantidades somadas.")
        List<ItemPedidoRequisicao> itens) {
}
