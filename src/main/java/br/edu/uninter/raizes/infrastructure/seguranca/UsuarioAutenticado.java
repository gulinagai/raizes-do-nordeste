package br.edu.uninter.raizes.infrastructure.seguranca;

import br.edu.uninter.raizes.domain.entidade.Usuario;
import br.edu.uninter.raizes.domain.enumeracao.Perfil;

public record UsuarioAutenticado(Long id, String nome, Perfil perfil) {

    public static UsuarioAutenticado de(Usuario usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getNome(), usuario.getPerfil());
    }
}
