package br.edu.uninter.raizes.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uninter.raizes.domain.entidade.Usuario;
import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConsultaUsuarioAutenticadoTest {

    private static final String SENHA = "Senha@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepositorio usuarios;

    @Autowired
    private SecretKey chaveJwt;

    @Test
    void retornaSomenteDadosPublicosDoUsuarioDoToken() throws Exception {
        String emailMaria = emailNovo();
        String emailJoao = emailNovo();
        String tokenMaria = tokenDe(emailMaria, "Maria Silva");
        String tokenJoao = tokenDe(emailJoao, "Joao Souza");
        long idMaria = usuarios.findByEmail(emailMaria).orElseThrow().getId();
        long idJoao = usuarios.findByEmail(emailJoao).orElseThrow().getId();

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + tokenMaria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idMaria))
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.perfil").value("CLIENTE"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(jsonPath("$.ativo").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist());

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + tokenJoao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idJoao))
                .andExpect(jsonPath("$.nome").value("Joao Souza"));
    }

    @Test
    void semTokenRetornaNaoAutenticado() throws Exception {
        esperarNaoAutenticado(mockMvc.perform(get("/auth/me")).andReturn());
    }

    @Test
    void tokenInvalidoRetornaNaoAutenticado() throws Exception {
        esperarNaoAutenticado(mockMvc.perform(get("/auth/me").header("Authorization", "Bearer nao-e-jwt"))
                .andReturn());
    }

    @Test
    void tokenExpiradoRetornaNaoAutenticado() throws Exception {
        String email = emailNovo();
        tokenDe(email, "Maria Silva");
        Usuario usuario = usuarios.findByEmail(email).orElseThrow();
        String token = Jwts.builder()
                .subject(usuario.getId().toString())
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(chaveJwt)
                .compact();

        esperarNaoAutenticado(mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andReturn());
    }

    @Test
    void usuarioInativoRetornaNaoAutenticado() throws Exception {
        String email = emailNovo();
        String token = tokenDe(email, "Maria Silva");
        Usuario usuario = usuarios.findByEmail(email).orElseThrow();
        usuario.setAtivo(false);
        usuarios.saveAndFlush(usuario);

        esperarNaoAutenticado(mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andReturn());
    }

    private void esperarNaoAutenticado(MvcResult resultado) throws Exception {
        assertEquals(401, resultado.getResponse().getStatus());
        assertEquals("NAO_AUTENTICADO", JsonPath.read(resultado.getResponse().getContentAsString(), "$.error"));
    }

    private String tokenDe(String email, String nome) throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"%s","email":"%s","senha":"%s"}
                                """.formatted(nome, email, SENHA)))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, SENHA)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    private String emailNovo() {
        return "me-" + UUID.randomUUID() + "@exemplo.com";
    }
}
