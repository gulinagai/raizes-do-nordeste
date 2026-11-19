package br.edu.uninter.raizes.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credenciais para obter um token JWT.")
public record LoginRequisicao(
        @NotBlank(message = "é obrigatório")
        @Email(message = "deve ser um e-mail válido")
        @Size(max = 180, message = "deve ter no máximo 180 caracteres")
        @Schema(description = "E-mail do usuário.", example = "cliente@raizes.test")
        String email,

        @NotBlank(message = "é obrigatória")
        @Size(max = 72, message = "deve ter no máximo 72 caracteres")
        @Schema(description = "Senha do usuário.", example = "Raizes@2026")
        String senha) {
}
