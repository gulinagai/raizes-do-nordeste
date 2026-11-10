package br.edu.uninter.raizes.api.controller;

import br.edu.uninter.raizes.api.dto.LoginRequisicao;
import br.edu.uninter.raizes.api.dto.LoginResposta;
import br.edu.uninter.raizes.api.dto.UsuarioResposta;
import br.edu.uninter.raizes.application.servico.AutenticacaoServico;
import br.edu.uninter.raizes.application.servico.AutenticacaoServico.ResultadoLogin;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AutenticacaoController {

    private final AutenticacaoServico autenticacaoServico;

    public AutenticacaoController(AutenticacaoServico autenticacaoServico) {
        this.autenticacaoServico = autenticacaoServico;
    }

    @PostMapping("/login")
    public LoginResposta login(@Valid @RequestBody LoginRequisicao requisicao) {
        ResultadoLogin resultado = autenticacaoServico.autenticar(requisicao.email(), requisicao.senha());
        return new LoginResposta(
                resultado.accessToken(),
                "Bearer",
                resultado.expiresIn(),
                UsuarioResposta.de(resultado.usuario()));
    }
}
