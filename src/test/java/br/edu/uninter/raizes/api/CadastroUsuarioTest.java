package br.edu.uninter.raizes.api;

import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uninter.raizes.domain.entidade.Usuario;
import br.edu.uninter.raizes.domain.enumeracao.Perfil;
import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CadastroUsuarioTest {

    private static final String SENHA = "Senha@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepositorio usuarios;

    @Autowired
    private PasswordEncoder codificadorDeSenha;

    @Test
    void cadastraClienteComSenhaEmBcrypt() throws Exception {
        String email = emailNovo();

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Maria Silva", email, SENHA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.perfil").value("CLIENTE"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(jsonPath("$.ativo").doesNotExist());

        Usuario salvo = usuarios.findByEmail(email).orElseThrow();
        assertEquals(Perfil.CLIENTE, salvo.getPerfil());
        assertTrue(salvo.isAtivo());
        assertNotEquals(SENHA, salvo.getSenhaHash());
        assertTrue(salvo.getSenhaHash().startsWith("$2"));
        assertTrue(codificadorDeSenha.matches(SENHA, salvo.getSenhaHash()));
    }

    @Test
    void emailRepetidoRetorna409() throws Exception {
        String email = emailNovo();
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Maria Silva", email, SENHA)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Outra Pessoa", email, SENHA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_JA_CADASTRADO"))
                .andExpect(jsonPath("$.message").value("E-mail já cadastrado."))
                .andExpect(jsonPath("$.details[0].field").value("email"))
                .andExpect(jsonPath("$.details[0].issue").value("já cadastrado"))
                .andExpect(jsonPath("$.path").value("/usuarios"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void nomeEmBrancoRetorna422() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("  ", emailNovo(), SENHA)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("VALIDACAO"))
                .andExpect(jsonPath("$.message").value("Os dados informados são inválidos."))
                .andExpect(jsonPath("$.details[?(@.field == 'nome')]").exists())
                .andExpect(jsonPath("$.path").value("/usuarios"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void emailInvalidoRetorna422() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Maria Silva", "maria-sem-arroba", SENHA)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("VALIDACAO"))
                .andExpect(jsonPath("$.details[?(@.field == 'email')].issue").value("deve ser um e-mail válido"))
                .andExpect(jsonPath("$.path").value("/usuarios"));
    }

    @Test
    void senhaAusenteRetorna422() throws Exception {
        String json = """
                {"nome":"Maria Silva","email":"%s"}
                """.formatted(emailNovo());

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("VALIDACAO"))
                .andExpect(jsonPath("$.details[?(@.field == 'senha')]").exists());
    }

    @Test
    void perfilEnviadoNoPayloadNaoElevaPrivilegio() throws Exception {
        String email = emailNovo();
        String json = """
                {"nome":"Maria Silva","email":"%s","senha":"%s","perfil":"ADMIN","ativo":false,"senhaHash":"texto-puro","id":999999}
                """.formatted(email, SENHA);

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("CLIENTE"))
                .andExpect(jsonPath("$.id").value(not(999999)))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        Usuario salvo = usuarios.findByEmail(email).orElseThrow();
        assertEquals(Perfil.CLIENTE, salvo.getPerfil());
        assertTrue(salvo.isAtivo());
        assertNotEquals(999999L, salvo.getId());
        assertNotEquals("texto-puro", salvo.getSenhaHash());
        assertTrue(codificadorDeSenha.matches(SENHA, salvo.getSenhaHash()));
        assertTrue(salvo.getSenhaHash().startsWith("$2"));
    }

    private String emailNovo() {
        return "cliente-" + UUID.randomUUID() + "@exemplo.com";
    }

    private String corpo(String nome, String email, String senha) {
        return """
                {"nome":"%s","email":"%s","senha":"%s"}
                """.formatted(nome, email, senha);
    }
}
