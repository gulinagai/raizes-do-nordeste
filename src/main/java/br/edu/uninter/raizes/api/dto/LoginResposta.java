package br.edu.uninter.raizes.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token de acesso e dados públicos do usuário autenticado.")
public record LoginResposta(
        @Schema(description = "Token JWT. Envie somente este valor no Swagger.", example = "eyJhbGciOiJIUzI1NiJ9")
        String accessToken,
        @Schema(description = "Tipo do token.", example = "Bearer")
        String tokenType,
        @Schema(description = "Validade do token, em segundos.", example = "3600")
        long expiresIn,
        @Schema(description = "Usuário autenticado, sem e-mail e sem senha.")
        UsuarioResposta user) {
}
