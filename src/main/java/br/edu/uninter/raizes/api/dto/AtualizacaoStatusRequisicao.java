package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.domain.enumeracao.StatusPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Próximo status operacional do pedido. A cozinha informa PRONTO e o atendimento informa ENTREGUE.")
public record AtualizacaoStatusRequisicao(
        @NotNull(message = "é obrigatório")
        @Schema(description = "Status de destino. A sequência aceita é EM_PREPARO para PRONTO e PRONTO para ENTREGUE.", example = "PRONTO")
        StatusPedido status) {
}
