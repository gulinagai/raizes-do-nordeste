package br.edu.uninter.raizes.application.servico;

import br.edu.uninter.raizes.domain.entidade.Usuario;
import br.edu.uninter.raizes.domain.enumeracao.AcaoAuditoria;
import br.edu.uninter.raizes.domain.enumeracao.Perfil;
import br.edu.uninter.raizes.domain.excecao.EmailJaCadastradoException;
import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastroUsuarioServico {

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificadorDeSenha;
    private final AuditoriaServico auditoria;

    public CadastroUsuarioServico(
            UsuarioRepositorio usuarios, PasswordEncoder codificadorDeSenha, AuditoriaServico auditoria) {
        this.usuarios = usuarios;
        this.codificadorDeSenha = codificadorDeSenha;
        this.auditoria = auditoria;
    }

    @Transactional
    public Usuario cadastrar(String nome, String email, String senha) {
        String nomeNormalizado = nome.trim();
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

        if (usuarios.existsByEmail(emailNormalizado)) {
            throw new EmailJaCadastradoException();
        }

        Usuario usuario = new Usuario(
                nomeNormalizado,
                emailNormalizado,
                codificadorDeSenha.encode(senha),
                Perfil.CLIENTE,
                true);

        try {
            Usuario salvo = usuarios.saveAndFlush(usuario);
            auditoria.registrar(null, AcaoAuditoria.CADASTRAR_USUARIO, "usuario", salvo.getId(), "CRIADO");
            return salvo;
        } catch (DataIntegrityViolationException excecao) {
            throw new EmailJaCadastradoException();
        }
    }
}
