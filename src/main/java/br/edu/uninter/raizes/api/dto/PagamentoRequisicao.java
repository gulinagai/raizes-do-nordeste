package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.domain.enumeracao.ResultadoSimulado;
import jakarta.validation.constraints.NotNull;

public record PagamentoRequisicao(
        @NotNull(message = "é obrigatório") Long pedidoId,
        @NotNull(message = "é obrigatório") ResultadoSimulado resultadoSimulado) {
}
