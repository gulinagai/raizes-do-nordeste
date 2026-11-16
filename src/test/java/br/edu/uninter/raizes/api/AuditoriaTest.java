package br.edu.uninter.raizes.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uninter.raizes.domain.entidade.Estoque;
import br.edu.uninter.raizes.domain.entidade.Produto;
import br.edu.uninter.raizes.domain.entidade.RegistroAuditoria;
import br.edu.uninter.raizes.domain.entidade.Unidade;
import br.edu.uninter.raizes.domain.enumeracao.AcaoAuditoria;
import br.edu.uninter.raizes.infrastructure.persistencia.EstoqueRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.ProdutoRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.RegistroAuditoriaRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UnidadeRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
import com.jayway.jsonpath.JsonPath;
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
class AuditoriaTest {

    private static final String SENHA = "Raizes@2026";
    private static final String CLIENTE = "cliente@raizes.test";
    private static final String COZINHA = "cozinha@raizes.test";
    private static final String GERENTE = "gerente@raizes.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistroAuditoriaRepositorio auditorias;

    @Autowired
    private UsuarioRepositorio usuarios;

    @Autowired
    private UnidadeRepositorio unidades;

    @Autowired
    private ProdutoRepositorio produtos;

    @Autowired
    private EstoqueRepositorio estoques;

    @Test
    void registraAcoesDoPedidoComOResponsavel() throws Exception {
        long clienteId = usuarios.findByEmail(CLIENTE).orElseThrow().getId();
        long cozinhaId = usuarios.findByEmail(COZINHA).orElseThrow().getId();
        long gerenteId = usuarios.findByEmail(GERENTE).orElseThrow().getId();
        Estoque estoque = estoque();

        mockMvc.perform(post("/estoque/movimentacoes")
                        .header("Authorization", "Bearer " + token(GERENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unidadeId":%d,"produtoId":%d,"tipo":"ENTRADA","quantidade":1}
                                """.formatted(estoque.getUnidade().getId(), estoque.getProduto().getId())))
                .andExpect(status().isCreated());

        MvcResult pedido = mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + token(CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unidadeId":%d,"clienteId":%d,"canalPedido":"APP","itens":[{"produtoId":%d,"quantidade":1}]}
                                """.formatted(estoque.getUnidade().getId(), clienteId, estoque.getProduto().getId())))
                .andExpect(status().isCreated())
                .andReturn();
        long pedidoId = numero(pedido.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/pagamentos")
                        .header("Authorization", "Bearer " + token(CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId":%d,"resultadoSimulado":"APROVADO"}
                                """.formatted(pedidoId)))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/pedidos/{id}/status", pedidoId)
                        .header("Authorization", "Bearer " + token(COZINHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PRONTO"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/pedidos/{id}/cancelamento", pedidoId)
                        .header("Authorization", "Bearer " + token(CLIENTE)))
                .andExpect(status().isOk());

        RegistroAuditoria entrada = auditorias.findAll().stream()
                .filter(item -> item.getAcao() == AcaoAuditoria.MOVIMENTAR_ESTOQUE)
                .filter(item -> estoque.getId().equals(item.getRecursoId()))
                .filter(item -> item.getUsuario().getId().equals(gerenteId))
                .findFirst()
                .orElseThrow();
        assertEquals("ENTRADA", entrada.getResultado());
        assertNotNull(entrada.getDataHora());
        assertNotNull(entrada.getDataHora());

        RegistroAuditoria criacao = unico(AcaoAuditoria.CRIAR_PEDIDO, pedidoId);
        assertEquals(clienteId, criacao.getUsuario().getId());
        assertEquals("pedido", criacao.getRecurso());
        assertEquals("CRIADO", criacao.getResultado());

        RegistroAuditoria pagamento = unico(AcaoAuditoria.PROCESSAR_PAGAMENTO, pedidoId);
        assertEquals(clienteId, pagamento.getUsuario().getId());
        assertEquals("APROVADO", pagamento.getResultado());

        RegistroAuditoria status = unico(AcaoAuditoria.ATUALIZAR_STATUS_PEDIDO, pedidoId);
        assertEquals(cozinhaId, status.getUsuario().getId());
        assertEquals("PRONTO", status.getResultado());

        RegistroAuditoria cancelamento = unico(AcaoAuditoria.CANCELAR_PEDIDO, pedidoId);
        assertEquals(clienteId, cancelamento.getUsuario().getId());
        assertEquals("CANCELADO", cancelamento.getResultado());
    }

    @Test
    void cadastroSemAutenticacaoNaoGuardaSenhaENaoRegistraFalha() throws Exception {
        MvcResult cadastro = mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Auditoria Publica","email":"auditoria.publica@raizes.test","senha":"%s"}
                                """.formatted(SENHA)))
                .andExpect(status().isCreated())
                .andReturn();
        long novoId = numero(cadastro.getResponse().getContentAsString(), "$.id");

        RegistroAuditoria registro = unico(AcaoAuditoria.CADASTRAR_USUARIO, novoId);
        assertNull(registro.getUsuario());
        assertEquals("usuario", registro.getRecurso());
        assertEquals("CRIADO", registro.getResultado());

        long clienteId = usuarios.findByEmail(CLIENTE).orElseThrow().getId();
        long antes = auditorias.findAll().stream()
                .filter(item -> item.getAcao() == AcaoAuditoria.CRIAR_PEDIDO)
                .count();
        Estoque estoque = estoque();
        mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + token(CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unidadeId":%d,"clienteId":%d,"canalPedido":"APP","itens":[{"produtoId":%d,"quantidade":1}]}
                                """.formatted(estoque.getUnidade().getId(), novoId, estoque.getProduto().getId())))
                .andExpect(status().isForbidden());
        long depois = auditorias.findAll().stream()
                .filter(item -> item.getAcao() == AcaoAuditoria.CRIAR_PEDIDO)
                .count();
        assertEquals(antes, depois);
        assertEquals(clienteId, usuarios.findByEmail(CLIENTE).orElseThrow().getId());
    }

    @Test
    void registraConsultaDePedidosEDoProprioUsuario() throws Exception {
        String acesso = token(CLIENTE);
        long clienteId = usuarios.findByEmail(CLIENTE).orElseThrow().getId();

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + acesso))
                .andExpect(status().isOk());
        mockMvc.perform(get("/pedidos").header("Authorization", "Bearer " + acesso))
                .andExpect(status().isOk());

        RegistroAuditoria consultaUsuario = unico(AcaoAuditoria.CONSULTAR_USUARIO, clienteId);
        assertEquals(clienteId, consultaUsuario.getUsuario().getId());
        assertEquals("CONSULTA", consultaUsuario.getResultado());

        RegistroAuditoria consultaPedidos = auditorias.findAll().stream()
                .filter(item -> item.getAcao() == AcaoAuditoria.CONSULTAR_PEDIDOS)
                .filter(item -> item.getUsuario().getId().equals(clienteId))
                .reduce((primeira, segunda) -> segunda)
                .orElseThrow();
        assertNull(consultaPedidos.getRecursoId());
        assertEquals("pedido", consultaPedidos.getRecurso());

        RegistroAuditoria autenticacao = auditorias.findAll().stream()
                .filter(item -> item.getAcao() == AcaoAuditoria.AUTENTICAR)
                .filter(item -> item.getUsuario().getId().equals(clienteId))
                .reduce((primeira, segunda) -> segunda)
                .orElseThrow();
        assertEquals("SUCESSO", autenticacao.getResultado());
        assertEquals(clienteId, autenticacao.getRecursoId());
    }

    private RegistroAuditoria unico(AcaoAuditoria acao, long recursoId) {
        return auditorias.findAll().stream()
                .filter(item -> item.getAcao() == acao)
                .filter(item -> recursoId == (item.getRecursoId() == null ? -1 : item.getRecursoId()))
                .reduce((primeira, segunda) -> segunda)
                .orElseThrow();
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

    private Estoque estoque() {
        Unidade unidade = unidades.findAll().stream()
                .filter(item -> item.getNome().equals("Raízes Matriz Recife"))
                .findFirst()
                .orElseThrow();
        Produto produto = produtos.findAll().stream()
                .filter(item -> item.getNome().equals("Baião de dois"))
                .findFirst()
                .orElseThrow();
        return estoques.findByUnidadeIdAndProdutoId(unidade.getId(), produto.getId()).orElseThrow();
    }

    private long numero(String corpo, String caminho) {
        return ((Number) JsonPath.read(corpo, caminho)).longValue();
    }
}
