package br.edu.uninter.raizes.api.dto;

import jakarta.validation.constraints.NotNull;

public record ConsentimentoRequisicao(
        @NotNull(message = "é obrigatório")
        Boolean consentimento) {
}
