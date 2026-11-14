package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.domain.enumeracao.StatusPedido;
import jakarta.validation.constraints.NotNull;

public record AtualizacaoStatusRequisicao(
        @NotNull(message = "é obrigatório") StatusPedido status) {
}
