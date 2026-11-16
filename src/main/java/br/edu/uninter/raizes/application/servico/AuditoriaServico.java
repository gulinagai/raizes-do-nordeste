package br.edu.uninter.raizes.application.servico;


import br.edu.uninter.raizes.domain.entidade.RegistroAuditoria;
import br.edu.uninter.raizes.domain.entidade.Usuario;
import br.edu.uninter.raizes.domain.enumeracao.AcaoAuditoria;
import br.edu.uninter.raizes.infrastructure.persistencia.RegistroAuditoriaRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaServico {

    private final RegistroAuditoriaRepositorio registros;
    private final UsuarioRepositorio usuarios;

    public AuditoriaServico(RegistroAuditoriaRepositorio registros, UsuarioRepositorio usuarios) {
        this.registros = registros;
        this.usuarios = usuarios;
    }

    @Transactional
    public void registrar(Long usuarioId, AcaoAuditoria acao, String recurso, Long recursoId, String resultado) {
        Usuario usuario = usuarioId == null ? null : usuarios.findById(usuarioId).orElse(null);
        registros.saveAndFlush(new RegistroAuditoria(usuario, acao, recurso, recursoId, resultado));
    }
}
