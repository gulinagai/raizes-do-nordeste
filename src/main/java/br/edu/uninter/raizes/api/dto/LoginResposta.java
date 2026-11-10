package br.edu.uninter.raizes.api.dto;

public record LoginResposta(String accessToken, String tokenType, long expiresIn, UsuarioResposta user) {
}
