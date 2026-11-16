package br.edu.uninter.raizes.application.servico;

import br.edu.uninter.raizes.domain.entidade.Fidelidade;
import br.edu.uninter.raizes.domain.entidade.Usuario;
import br.edu.uninter.raizes.domain.enumeracao.AcaoAuditoria;
import br.edu.uninter.raizes.domain.enumeracao.Perfil;
import br.edu.uninter.raizes.domain.excecao.AcessoNegadoException;
import br.edu.uninter.raizes.domain.excecao.RecursoNaoEncontradoException;
import br.edu.uninter.raizes.infrastructure.persistencia.FidelidadeRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FidelidadeServico {

    private final FidelidadeRepositorio fidelidades;
    private final UsuarioRepositorio usuarios;
    private final AuditoriaServico auditoria;

    public FidelidadeServico(
            FidelidadeRepositorio fidelidades, UsuarioRepositorio usuarios, AuditoriaServico auditoria) {
        this.fidelidades = fidelidades;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public SituacaoFidelidade consultar(Long usuarioId) {
        return fidelidades.findByUsuarioId(usuarioId)
                .map(SituacaoFidelidade::de)
                .orElseGet(SituacaoFidelidade::ausente);
    }

    @Transactional
    public SituacaoFidelidade registrar(Long usuarioId, boolean consentimento) {
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow(RecursoNaoEncontradoException::new);
        if (usuario.getPerfil() != Perfil.CLIENTE) {
            throw new AcessoNegadoException();
        }
        Optional<Fidelidade> existente = fidelidades.findByUsuarioId(usuarioId);
        boolean haviaConsentimento = existente.map(Fidelidade::isConsentimento).orElse(false);
        Fidelidade conta;
        boolean mudou;
        if (existente.isEmpty()) {
            conta = new Fidelidade(usuario, consentimento);
            mudou = true;
        } else {
            conta = existente.get();
            mudou = conta.definirConsentimento(consentimento);
        }
        Fidelidade salva = fidelidades.saveAndFlush(conta);
        if (mudou) {
            auditoria.registrar(
                    usuarioId,
                    AcaoAuditoria.REGISTRAR_CONSENTIMENTO,
                    "fidelidade",
                    salva.getId(),
                    consentimento ? "ACEITO" : (haviaConsentimento ? "REVOGADO" : "RECUSADO"));
        }
        return SituacaoFidelidade.de(salva);
    }
}
