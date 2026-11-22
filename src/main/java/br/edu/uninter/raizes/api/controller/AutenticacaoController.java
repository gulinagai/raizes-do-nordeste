package br.edu.uninter.raizes.api.controller;

import br.edu.uninter.raizes.api.dto.LoginRequisicao;
import br.edu.uninter.raizes.api.dto.LoginResposta;
import br.edu.uninter.raizes.api.dto.UsuarioResposta;
import br.edu.uninter.raizes.api.erro.ErroResposta;
import br.edu.uninter.raizes.application.servico.AuditoriaServico;
import br.edu.uninter.raizes.application.servico.AutenticacaoServico;
import br.edu.uninter.raizes.application.servico.AutenticacaoServico.ResultadoLogin;
import br.edu.uninter.raizes.domain.enumeracao.AcaoAuditoria;
import br.edu.uninter.raizes.infrastructure.seguranca.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Login e consulta do usuário autenticado.")
public class AutenticacaoController {

    private final AutenticacaoServico autenticacaoServico;
    private final AuditoriaServico auditoria;

    public AutenticacaoController(AutenticacaoServico autenticacaoServico, AuditoriaServico auditoria) {
        this.autenticacaoServico = autenticacaoServico;
        this.auditoria = auditoria;
    }

    @Operation(
            summary = "Entrar",
            description = "Valida e-mail e senha e devolve um JWT. A operação é pública. O token não é gravado na auditoria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciais aceitas."),
            @ApiResponse(
                    responseCode = "401",
                    description = "E-mail ou senha não conferem. O código do corpo é CREDENCIAIS_INVALIDAS.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "E-mail ou senha ausentes, ou JSON inválido. O código do corpo é VALIDACAO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    })
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

    @Operation(
            summary = "Consultar usuário autenticado",
            description = "Devolve o identificador, o nome e o perfil do token. Qualquer perfil autenticado pode consultar a si mesmo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário do token."),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido. O código do corpo é NAO_AUTENTICADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public UsuarioResposta eu(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        auditoria.registrar(
                autenticado.id(), AcaoAuditoria.CONSULTAR_USUARIO, "usuario", autenticado.id(), "CONSULTA");
        return new UsuarioResposta(autenticado.id(), autenticado.nome(), autenticado.perfil());
    }
}
