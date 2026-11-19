package br.edu.uninter.raizes.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para cadastrar um cliente. O perfil gravado é sempre CLIENTE.")
public record CadastroUsuarioRequisicao(
        @NotBlank(message = "é obrigatório")
        @Size(max = 120, message = "deve ter no máximo 120 caracteres")
        @Schema(description = "Nome do cliente.", example = "Maria Silva")
        String nome,

        @NotBlank(message = "é obrigatório")
        @Email(message = "deve ser um e-mail válido")
        @Size(max = 180, message = "deve ter no máximo 180 caracteres")
        @Schema(description = "E-mail único do cliente.", example = "maria.silva@raizes.test")
        String email,

        @NotBlank(message = "é obrigatória")
        @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
        @Schema(description = "Senha em texto. A API armazena apenas o hash.", example = "Raizes@2026")
        String senha) {
}
