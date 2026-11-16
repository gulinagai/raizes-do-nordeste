package br.edu.uninter.raizes.api.controller;

import br.edu.uninter.raizes.api.dto.LoginRequisicao;
import br.edu.uninter.raizes.api.dto.LoginResposta;
import br.edu.uninter.raizes.api.dto.UsuarioResposta;
import br.edu.uninter.raizes.application.servico.AuditoriaServico;
import br.edu.uninter.raizes.application.servico.AutenticacaoServico;
import br.edu.uninter.raizes.application.servico.AutenticacaoServico.ResultadoLogin;
import br.edu.uninter.raizes.domain.enumeracao.AcaoAuditoria;
import br.edu.uninter.raizes.infrastructure.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AutenticacaoController {

    private final AutenticacaoServico autenticacaoServico;
    private final AuditoriaServico auditoria;

    public AutenticacaoController(AutenticacaoServico autenticacaoServico, AuditoriaServico auditoria) {
        this.autenticacaoServico = autenticacaoServico;
        this.auditoria = auditoria;
    }

    @PostMapping("/login")
    public LoginResposta login(@Valid @RequestBody LoginRequisicao requisicao) {
        ResultadoLogin resultado = autenticacaoServico.autenticar(requisicao.email(), requisicao.senha());
        auditoria.registrar(
                resultado.usuario().getId(),
                AcaoAuditoria.AUTENTICAR,
                "usuario",
                resultado.usuario().getId(),
                "SUCESSO");
        return new LoginResposta(
                resultado.accessToken(),
                "Bearer",
                resultado.expiresIn(),
                UsuarioResposta.de(resultado.usuario()));
    }

    @GetMapping("/me")
    public UsuarioResposta eu(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
        auditoria.registrar(
                autenticado.id(), AcaoAuditoria.CONSULTAR_USUARIO, "usuario", autenticado.id(), "CONSULTA");
        return new UsuarioResposta(autenticado.id(), autenticado.nome(), autenticado.perfil());
    }
}
