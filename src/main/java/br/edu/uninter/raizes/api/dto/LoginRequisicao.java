package br.edu.uninter.raizes.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequisicao(
        @NotBlank(message = "é obrigatório")
        @Email(message = "deve ser um e-mail válido")
        @Size(max = 180, message = "deve ter no máximo 180 caracteres")
        String email,

        @NotBlank(message = "é obrigatória")
        @Size(max = 72, message = "deve ter no máximo 72 caracteres")
        String senha) {
}
