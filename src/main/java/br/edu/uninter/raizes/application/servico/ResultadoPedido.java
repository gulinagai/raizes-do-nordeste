package br.edu.uninter.raizes.application.servico;

import br.edu.uninter.raizes.domain.enumeracao.CanalPedido;
import br.edu.uninter.raizes.domain.enumeracao.StatusPedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ResultadoPedido(
        Long id,
        Long unidadeId,
        Long clienteId,
        CanalPedido canalPedido,
        StatusPedido status,
        BigDecimal total,
        List<ItemDoPedido> itens,
        Instant dataCriacao) {
}
