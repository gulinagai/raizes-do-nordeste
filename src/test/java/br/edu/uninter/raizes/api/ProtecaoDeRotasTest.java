package br.edu.uninter.raizes.api;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uninter.raizes.domain.entidade.Usuario;
import br.edu.uninter.raizes.domain.enumeracao.Perfil;
import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
import br.edu.uninter.raizes.infrastructure.seguranca.UsuarioAutenticado;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ProtecaoDeRotasTest.RecursoDeTeste.class)
@Transactional
class ProtecaoDeRotasTest {

    private static final String SENHA = "Senha@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepositorio usuarios;

    @Autowired
    private SecretKey chaveJwt;

    @Test
    void cadastroELoginContinuamPublicos() throws Exception {
        String email = emailNovo();
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastro(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login(email, "senha-errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("CREDENCIAIS_INVALIDAS"));
    }

    @Test
    void documentacaoNaoExigeToken() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NAO_ENCONTRADO"));
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NAO_ENCONTRADO"));
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NAO_ENCONTRADO"));
    }

    @Test
    void tokenValidoIdentificaUsuarioEPerfilDoBanco() throws Exception {
        String email = emailNovo();
        String token = tokenDe(email);

        Usuario usuario = usuarios.findByEmail(email).orElseThrow();
        usuario.setPerfil(Perfil.GERENTE);
        usuarios.saveAndFlush(usuario);

        mockMvc.perform(get("/teste/autenticacao").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuario.getId()))
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.perfil").value("GERENTE"));

        mockMvc.perform(get("/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("METODO_NAO_PERMITIDO"));
    }

    @Test
    void rotaProtegidaSemTokenRetornaNaoAutenticado() throws Exception {
        esperarNaoAutenticado(mockMvc.perform(get("/teste/autenticacao")).andReturn());
    }

    @Test
    void tokenMalformadoRetornaNaoAutenticado() throws Exception {
        esperarNaoAutenticado(mockMvc.perform(get("/teste/autenticacao").header("Authorization", "Bearer nao-e-jwt"))
                .andReturn());
    }

    @Test
    void assinaturaInvalidaRetornaNaoAutenticado() throws Exception {
        String email = emailNovo();
        tokenDe(email);
        Usuario usuario = usuarios.findByEmail(email).orElseThrow();
        SecretKey outraChave = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "outra-chave-de-teste-com-32-bytes!!".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject(usuario.getId().toString())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(outraChave)
                .compact();

        esperarNaoAutenticado(mockMvc.perform(get("/teste/autenticacao").header("Authorization", "Bearer " + token))
                .andReturn());
    }

    @Test
    void tokenExpiradoRetornaNaoAutenticado() throws Exception {
        String email = emailNovo();
        tokenDe(email);
        Usuario usuario = usuarios.findByEmail(email).orElseThrow();
        String token = Jwts.builder()
                .subject(usuario.getId().toString())
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(chaveJwt)
                .compact();

        esperarNaoAutenticado(mockMvc.perform(get("/teste/autenticacao").header("Authorization", "Bearer " + token))
                .andReturn());
    }

    @Test
    void usuarioInexistenteRetornaNaoAutenticado() throws Exception {
        String token = Jwts.builder()
                .subject("999999999")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(chaveJwt)
                .compact();

        esperarNaoAutenticado(mockMvc.perform(get("/teste/autenticacao").header("Authorization", "Bearer " + token))
                .andReturn());
    }

    @Test
    void usuarioInativoRetornaNaoAutenticado() throws Exception {
        String email = emailNovo();
        String token = tokenDe(email);
        Usuario usuario = usuarios.findByEmail(email).orElseThrow();
        usuario.setAtivo(false);
        usuarios.saveAndFlush(usuario);

        esperarNaoAutenticado(mockMvc.perform(get("/teste/autenticacao").header("Authorization", "Bearer " + token))
                .andReturn());
    }

    @Test
    void outrosMetodosDeUsuariosExigemToken() throws Exception {
        esperarNaoAutenticado(mockMvc.perform(get("/usuarios")).andReturn());
    }

    private void esperarNaoAutenticado(MvcResult resultado) throws Exception {
        assertNotEquals(200, resultado.getResponse().getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(401, resultado.getResponse().getStatus());
        String corpo = resultado.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals("NAO_AUTENTICADO", JsonPath.read(corpo, "$.error"));
        org.junit.jupiter.api.Assertions.assertEquals("Autenticação necessária.", JsonPath.read(corpo, "$.message"));
        org.junit.jupiter.api.Assertions.assertTrue(JsonPath.read(corpo, "$.details") instanceof java.util.List<?> detalhes
                && detalhes.isEmpty());
        org.junit.jupiter.api.Assertions.assertFalse(corpo.contains("JWT"));
        org.junit.jupiter.api.Assertions.assertFalse(corpo.contains("expired"));
        org.junit.jupiter.api.Assertions.assertFalse(corpo.contains("Exception"));
    }

    private String tokenDe(String email) throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastro(email)))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login(email, SENHA)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    private String emailNovo() {
        return "protecao-" + UUID.randomUUID() + "@exemplo.com";
    }

    private String cadastro(String email) {
        return """
                {"nome":"Maria Silva","email":"%s","senha":"%s"}
                """.formatted(email, SENHA);
    }

    private String login(String email, String senha) {
        return """
                {"email":"%s","senha":"%s"}
                """.formatted(email, senha);
    }

    @RestController
    static class RecursoDeTeste {

        @GetMapping("/teste/autenticacao")
        UsuarioAutenticado atual(@AuthenticationPrincipal UsuarioAutenticado usuario) {
            return usuario;
        }
    }
}
