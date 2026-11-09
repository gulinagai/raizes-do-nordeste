package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.domain.entidade.Usuario;
import br.edu.uninter.raizes.domain.enumeracao.Perfil;

public record UsuarioResposta(Long id, String nome, Perfil perfil) {

    public static UsuarioResposta de(Usuario usuario) {
        return new UsuarioResposta(usuario.getId(), usuario.getNome(), usuario.getPerfil());
    }
}
