package br.edu.uninter.raizes.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import javax.crypto.SecretKey;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuariosDemonstracaoTest {

    private static final String SENHA = "Raizes@2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey chaveJwt;

    @ParameterizedTest
    @CsvSource({
            "admin@raizes.test,Administrador Raízes,ADMIN",
            "gerente@raizes.test,Gerente Raízes,GERENTE",
            "atendente@raizes.test,Atendente Raízes,ATENDENTE",
            "cozinha@raizes.test,Operador Cozinha,COZINHA",
            "cliente@raizes.test,Cliente Demonstração,CLIENTE"
    })
    void loginEConsultaDoPerfil(String email, String nome, String perfil) throws Exception {
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.nome").value(nome))
                .andExpect(jsonPath("$.user.perfil").value(perfil))
                .andExpect(jsonPath("$.user.senha").doesNotExist())
                .andExpect(jsonPath("$.user.senhaHash").doesNotExist())
                .andReturn();

        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        String perfilNoToken = Jwts.parser().verifyWith(chaveJwt).build().parseSignedClaims(token).getPayload().get("perfil", String.class);
        assertEquals(perfil, perfilNoToken);

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value(nome))
                .andExpect(jsonPath("$.perfil").value(perfil))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }
}