package br.edu.uninter.raizes.api.controller;

import br.edu.uninter.raizes.api.dto.CadastroUsuarioRequisicao;
import br.edu.uninter.raizes.api.dto.UsuarioResposta;
import br.edu.uninter.raizes.application.servico.CadastroUsuarioServico;
import br.edu.uninter.raizes.domain.entidade.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final CadastroUsuarioServico cadastroUsuarioServico;

    public UsuarioController(CadastroUsuarioServico cadastroUsuarioServico) {
        this.cadastroUsuarioServico = cadastroUsuarioServico;
    }

    @PostMapping
    public ResponseEntity<UsuarioResposta> cadastrar(@Valid @RequestBody CadastroUsuarioRequisicao requisicao) {
        Usuario usuario = cadastroUsuarioServico.cadastrar(requisicao.nome(), requisicao.email(), requisicao.senha());
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResposta.de(usuario));
    }
}
