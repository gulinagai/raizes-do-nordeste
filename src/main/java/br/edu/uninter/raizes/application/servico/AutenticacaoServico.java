package br.edu.uninter.raizes.application.servico;

import br.edu.uninter.raizes.domain.entidade.Usuario;
import br.edu.uninter.raizes.domain.excecao.CredenciaisInvalidasException;
import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
import br.edu.uninter.raizes.infrastructure.seguranca.EmissorJwt;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoServico {

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificadorDeSenha;
    private final EmissorJwt emissorJwt;
    private final String hashParaTempoConstante;

    public AutenticacaoServico(
            UsuarioRepositorio usuarios, PasswordEncoder codificadorDeSenha, EmissorJwt emissorJwt) {
        this.usuarios = usuarios;
        this.codificadorDeSenha = codificadorDeSenha;
        this.emissorJwt = emissorJwt;
        this.hashParaTempoConstante = codificadorDeSenha.encode("tempo-constante");
    }

    @Transactional(readOnly = true)
    public ResultadoLogin autenticar(String email, String senha) {
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        Optional<Usuario> encontrado = usuarios.findByEmail(emailNormalizado);
        String hash = encontrado.map(Usuario::getSenhaHash).orElse(hashParaTempoConstante);
        boolean senhaConfere = codificadorDeSenha.matches(senha, hash);

        if (encontrado.isEmpty() || !encontrado.get().isAtivo() || !senhaConfere) {
            throw new CredenciaisInvalidasException();
        }

        Usuario usuario = encontrado.get();
        return new ResultadoLogin(emissorJwt.emitir(usuario), emissorJwt.expiracaoSegundos(), usuario);
    }

    public record ResultadoLogin(String accessToken, long expiresIn, Usuario usuario) {
    }
}
