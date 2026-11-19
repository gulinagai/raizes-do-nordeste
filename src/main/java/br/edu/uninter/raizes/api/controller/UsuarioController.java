package br.edu.uninter.raizes.api.controller;

import br.edu.uninter.raizes.api.dto.CadastroUsuarioRequisicao;
import br.edu.uninter.raizes.api.dto.UsuarioResposta;
import br.edu.uninter.raizes.api.erro.ErroResposta;
import br.edu.uninter.raizes.application.servico.CadastroUsuarioServico;
import br.edu.uninter.raizes.domain.entidade.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuários", description = "Cadastro público de clientes.")
public class UsuarioController {

    private final CadastroUsuarioServico cadastroUsuarioServico;

    public UsuarioController(CadastroUsuarioServico cadastroUsuarioServico) {
        this.cadastroUsuarioServico = cadastroUsuarioServico;
    }

    @Operation(
            summary = "Cadastrar cliente",
            description = """
                    Cria um usuário com perfil CLIENTE e senha armazenada como hash. \
                    A operação é pública. O perfil enviado no corpo, se houver, é ignorado.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente cadastrado."),
            @ApiResponse(
                    responseCode = "409",
                    description = "E-mail já cadastrado. O código do corpo é EMAIL_JA_CADASTRADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "Campo ausente, e-mail inválido ou JSON inválido. O código do corpo é VALIDACAO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    })
    @PostMapping
    public ResponseEntity<UsuarioResposta> cadastrar(@Valid @RequestBody CadastroUsuarioRequisicao requisicao) {
        Usuario usuario = cadastroUsuarioServico.cadastrar(requisicao.nome(), requisicao.email(), requisicao.senha());
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResposta.de(usuario));
    }
}
