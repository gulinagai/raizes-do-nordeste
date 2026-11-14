package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.domain.enumeracao.CanalPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PedidoRequisicao(
        @NotNull(message = "é obrigatório") Long unidadeId,
        @NotNull(message = "é obrigatório") Long clienteId,
        @NotNull(message = "é obrigatório") CanalPedido canalPedido,
        @NotNull(message = "é obrigatório") @NotEmpty(message = "deve conter ao menos um item") @Valid List<ItemPedidoRequisicao> itens) {
}
