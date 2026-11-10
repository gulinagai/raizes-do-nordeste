package br.edu.uninter.raizes.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uninter.raizes.domain.entidade.Usuario;
import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoginTest {

    private static final String SENHA = "Senha@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepositorio usuarios;

    @Autowired
    private SecretKey chaveJwt;

    @Test
    void loginValidoRetornaJwtAssinado() throws Exception {
        String email = emailNovo();
        long id = cadastrar(email, "Maria Silva");

        MvcResult resultado = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login(email, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.id").value(id))
                .andExpect(jsonPath("$.user.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.user.perfil").value("CLIENTE"))
                .andExpect(jsonPath("$.user.email").doesNotExist())
                .andExpect(jsonPath("$.user.senha").doesNotExist())
                .andExpect(jsonPath("$.user.senhaHash").doesNotExist())
                .andExpect(jsonPath("$.user.ativo").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andReturn();

        String token = JsonPath.read(resultado.getResponse().getContentAsString(), "$.accessToken");
        Claims claims = Jwts.parser().verifyWith(chaveJwt).build().parseSignedClaims(token).getPayload();
        assertEquals(Long.toString(id), claims.getSubject());
        assertEquals("CLIENTE", claims.get("perfil", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
        assertNull(claims.get("senha"));
        assertNull(claims.get("senhaHash"));

        String adulterado = token.substring(0, token.length() - 2) + "xx";
        assertThrows(JwtException.class, () -> Jwts.parser().verifyWith(chaveJwt).build().parseSignedClaims(adulterado));
    }

    @Test
    void emailInexistenteRetorna401Generico() throws Exception {
        esperarCredenciaisInvalidas(login(emailNovo(), SENHA));
    }

    @Test
    void senhaIncorretaRetorna401SemToken() throws Exception {
        String email = emailNovo();
        cadastrar(email, "Maria Silva");
        MvcResult resultado = esperarCredenciaisInvalidas(login(email, "SenhaErrada1"));
        assertTrue(!resultado.getResponse().getContentAsString().contains("accessToken"));
    }

    @Test
    void usuarioInativoRetorna401Generico() throws Exception {
        String email = emailNovo();
        cadastrar(email, "Maria Silva");
        Usuario usuario = usuarios.findByEmail(email).orElseThrow();
        usuario.setAtivo(false);
        usuarios.saveAndFlush(usuario);

        esperarCredenciaisInvalidas(login(email, SENHA));
    }

    @Test
    void semEmailRetorna422() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"senha":"%s"}
                                """.formatted(SENHA)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("VALIDACAO"))
                .andExpect(jsonPath("$.details[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.path").value("/auth/login"));
    }

    @Test
    void semSenhaRetorna422() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(emailNovo())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("VALIDACAO"))
                .andExpect(jsonPath("$.details[?(@.field == 'senha')]").exists())
                .andExpect(jsonPath("$.path").value("/auth/login"));
    }

    private long cadastrar(String email, String nome) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"%s","email":"%s","senha":"%s"}
                                """.formatted(nome, email, SENHA)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private MvcResult esperarCredenciaisInvalidas(String corpo) throws Exception {
        return mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("CREDENCIAIS_INVALIDAS"))
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.path").value("/auth/login"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.requestId").exists())
                .andReturn();
    }

    private String emailNovo() {
        return "login-" + UUID.randomUUID() + "@exemplo.com";
    }

    private String login(String email, String senha) {
        return """
                {"email":"%s","senha":"%s"}
                """.formatted(email, senha);
    }
}
