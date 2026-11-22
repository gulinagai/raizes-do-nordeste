package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.domain.entidade.Usuario;
import br.edu.uninter.raizes.domain.enumeracao.Perfil;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados públicos do usuário. E-mail, senha e situação da conta não são devolvidos.")
public record UsuarioResposta(
        @Schema(description = "Identificador do usuário.", example = "5")
        Long id,
        @Schema(description = "Nome do usuário.", example = "Cliente Demonstração")
        String nome,
        @Schema(description = "Perfil usado na autorização.", example = "CLIENTE")
        Perfil perfil) {

    public static UsuarioResposta de(Usuario usuario) {
        return new UsuarioResposta(usuario.getId(), usuario.getNome(), usuario.getPerfil());
    }
}
