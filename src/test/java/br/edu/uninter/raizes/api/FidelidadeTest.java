package br.edu.uninter.raizes.api;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uninter.raizes.domain.entidade.Fidelidade;
import br.edu.uninter.raizes.domain.entidade.RegistroAuditoria;
import br.edu.uninter.raizes.domain.enumeracao.AcaoAuditoria;
import br.edu.uninter.raizes.infrastructure.persistencia.FidelidadeRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.RegistroAuditoriaRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
import com.jayway.jsonpath.JsonPath;
import java.util.Comparator;
import java.util.List;
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
class FidelidadeTest {

    private static final String SENHA = "Raizes@2026";
    private static final String CLIENTE = "cliente@raizes.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepositorio usuarios;

    @Autowired
    private FidelidadeRepositorio fidelidades;

    @Autowired
    private RegistroAuditoriaRepositorio auditorias;

    @Test
    void registraConsentimentoComSaldoZero() throws Exception {
        long clienteId = usuarios.findByEmail(CLIENTE).orElseThrow().getId();
        String token = token(CLIENTE);

        mockMvc.perform(put("/fidelidade")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentimento\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consentimento").value(true))
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.dataConsentimento").exists())
                .andExpect(jsonPath("$.dataRevogacao").value(nullValue()));

        Fidelidade salva = fidelidades.findByUsuarioId(clienteId).orElseThrow();
        assertTrue(salva.isConsentimento());
        assertEquals(0, salva.getSaldo());
        assertNotNull(salva.getDataConsentimento());
        assertNull(salva.getDataRevogacao());

        List<RegistroAuditoria> eventos = consentimentos(clienteId);
        assertEquals(1, eventos.size());
        assertEquals("fidelidade", eventos.get(0).getRecurso());
        assertEquals(salva.getId(), eventos.get(0).getRecursoId());
        assertEquals("ACEITO", eventos.get(0).getResultado());
        assertNotNull(eventos.get(0).getDataHora());
    }

    @Test
    void revogaConsentimentoMantendoSaldo() throws Exception {
        String token = token(CLIENTE);
        long clienteId = usuarios.findByEmail(CLIENTE).orElseThrow().getId();

        MvcResult aceite = mockMvc.perform(put("/fidelidade")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentimento\":true}"))
                .andExpect(status().isOk())
                .andReturn();
        String concedidoEm = JsonPath.read(aceite.getResponse().getContentAsString(), "$.dataConsentimento");

        mockMvc.perform(put("/fidelidade")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentimento\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consentimento").value(false))
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.dataConsentimento").value(concedidoEm))
                .andExpect(jsonPath("$.dataRevogacao").exists());

        Fidelidade salva = fidelidades.findByUsuarioId(clienteId).orElseThrow();
        assertFalse(salva.isConsentimento());
        assertEquals(0, salva.getSaldo());
        List<RegistroAuditoria> eventos = consentimentos(clienteId);
        assertEquals(2, eventos.size());
        assertEquals("REVOGADO", eventos.get(1).getResultado());
    }

    @Test
    void consultaSaldoZeroAntesDoConsentimento() throws Exception {
        long clienteId = usuarios.findByEmail(CLIENTE).orElseThrow().getId();

        mockMvc.perform(get("/fidelidade")
                        .header("Authorization", "Bearer " + token(CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consentimento").value(false))
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.dataConsentimento").value(nullValue()))
                .andExpect(jsonPath("$.dataRevogacao").value(nullValue()));

        assertTrue(fidelidades.findByUsuarioId(clienteId).isEmpty());
    }

    @Test
    void consentimentoAusenteRetorna422() throws Exception {
        mockMvc.perform(put("/fidelidade")
                        .header("Authorization", "Bearer " + token(CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("VALIDACAO"))
                .andExpect(jsonPath("$.details[?(@.field == 'consentimento')].issue").value("é obrigatório"));
    }

    @Test
    void acessoExigeClienteAutenticado() throws Exception {
        mockMvc.perform(get("/fidelidade"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NAO_AUTENTICADO"));

        mockMvc.perform(put("/fidelidade")
                        .header("Authorization", "Bearer " + token("gerente@raizes.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentimento\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACESSO_NEGADO"));
    }

    private List<RegistroAuditoria> consentimentos(long clienteId) {
        return auditorias.findAll().stream()
                .filter(item -> item.getAcao() == AcaoAuditoria.REGISTRAR_CONSENTIMENTO)
                .filter(item -> item.getUsuario().getId().equals(clienteId))
                .sorted(Comparator.comparing(RegistroAuditoria::getId))
                .toList();
    }

    private String token(String email) throws Exception {
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, SENHA)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }
}
