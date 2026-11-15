package br.edu.uninter.raizes.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uninter.raizes.domain.entidade.Estoque;
import br.edu.uninter.raizes.domain.entidade.Produto;
import br.edu.uninter.raizes.domain.entidade.Unidade;
import br.edu.uninter.raizes.domain.enumeracao.StatusPedido;
import br.edu.uninter.raizes.infrastructure.persistencia.EstoqueRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.PedidoRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.ProdutoRepositorio;
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
class AutorizacaoTest {

    private static final String SENHA = "Raizes@2026";
    private static final String CLIENTE = "cliente@raizes.test";
    private static final String COZINHA = "cozinha@raizes.test";
    private static final String ATENDENTE = "atendente@raizes.test";
    private static final String GERENTE = "gerente@raizes.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepositorio usuarios;

    @Autowired
    private UnidadeRepositorio unidades;

    @Autowired
    private ProdutoRepositorio produtos;

    @Autowired
    private EstoqueRepositorio estoques;

    @Autowired
    private PedidoRepositorio pedidos;

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(post("/estoque/movimentacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NAO_AUTENTICADO"));
    }

    @Test
    void perfilSemPermissaoRecebe403() throws Exception {
        String movimento = movimento();
        esperarNegado(enviar("/estoque/movimentacoes", token(CLIENTE), movimento));
        esperarNegado(enviar("/estoque/movimentacoes", token(COZINHA), movimento));
        esperarNegado(enviar("/estoque/movimentacoes", token(ATENDENTE), movimento));
        esperarNegado(enviar("/pagamentos", token(COZINHA), """
                {"pedidoId":1,"resultadoSimulado":"APROVADO"}
                """));

        long pedidoId = criar(CLIENTE, usuarios.findByEmail(CLIENTE).orElseThrow().getId());
        esperarNegado(mockMvc.perform(patch("/pedidos/{id}/status", pedidoId)
                        .header("Authorization", "Bearer " + token(CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PRONTO"}
                                """))
                .andReturn());
    }

    @Test
    void perfilAutorizadoExecutaSuaOperacao() throws Exception {
        Estoque estoque = estoque();
        mockMvc.perform(post("/estoque/movimentacoes")
                        .header("Authorization", "Bearer " + token(GERENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unidadeId":%d,"produtoId":%d,"tipo":"ENTRADA","quantidade":1}
                                """.formatted(estoque.getUnidade().getId(), estoque.getProduto().getId())))
                .andExpect(status().isCreated());

        long pedidoId = criar(CLIENTE, usuarios.findByEmail(CLIENTE).orElseThrow().getId());
        mockMvc.perform(post("/pagamentos")
                        .header("Authorization", "Bearer " + token(CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId":%d,"resultadoSimulado":"APROVADO"}
                                """.formatted(pedidoId)))
                .andExpect(status().isCreated());

        esperarNegado(atualizar(pedidoId, "ENTREGUE", COZINHA));
        mockMvc.perform(patch("/pedidos/{id}/status", pedidoId)
                        .header("Authorization", "Bearer " + token(COZINHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PRONTO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRONTO"));

        esperarNegado(atualizar(pedidoId, "PRONTO", ATENDENTE));
        mockMvc.perform(patch("/pedidos/{id}/status", pedidoId)
                        .header("Authorization", "Bearer " + token(ATENDENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ENTREGUE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"));
    }

    @Test
    void clienteNaoOperaPedidoDeOutroCliente() throws Exception {
        long outroId = cadastrar("outro.auth@raizes.test");
        long clienteId = usuarios.findByEmail(CLIENTE).orElseThrow().getId();
        Estoque estoque = estoque();

        mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + token(CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(estoque, outroId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACESSO_NEGADO"));

        long pedidoId = criar(CLIENTE, clienteId);
        esperarNegado(mockMvc.perform(patch("/pedidos/{id}/cancelamento", pedidoId)
                        .header("Authorization", "Bearer " + token("outro.auth@raizes.test")))
                .andReturn());
        esperarNegado(enviar("/pagamentos", token("outro.auth@raizes.test"), """
                {"pedidoId":%d,"resultadoSimulado":"APROVADO"}
                """.formatted(pedidoId)));
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, pedidos.findById(pedidoId).orElseThrow().getStatus());
    }

    private MvcResult enviar(String caminho, String token, String corpo) throws Exception {
        return mockMvc.perform(post(caminho)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andReturn();
    }

    private MvcResult atualizar(long pedidoId, String situacao, String email) throws Exception {
        return mockMvc.perform(patch("/pedidos/{id}/status", pedidoId)
                        .header("Authorization", "Bearer " + token(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"%s"}
                                """.formatted(situacao)))
                .andReturn();
    }

    private void esperarNegado(MvcResult resultado) throws Exception {
        assertEquals(403, resultado.getResponse().getStatus());
        assertEquals("ACESSO_NEGADO", JsonPath.read(resultado.getResponse().getContentAsString(), "$.error"));
    }

    private long criar(String email, long clienteId) throws Exception {
        MvcResult pedido = mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + token(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(estoque(), clienteId)))
                .andExpect(status().isCreated())
                .andReturn();
        return numero(pedido.getResponse().getContentAsString(), "$.id");
    }

    private String pedido(Estoque estoque, long clienteId) {
        return """
                {"unidadeId":%d,"clienteId":%d,"canalPedido":"APP","itens":[{"produtoId":%d,"quantidade":1}]}
                """.formatted(estoque.getUnidade().getId(), clienteId, estoque.getProduto().getId());
    }

    private String movimento() {
        Estoque estoque = estoque();
        return """
                {"unidadeId":%d,"produtoId":%d,"tipo":"ENTRADA","quantidade":1}
                """.formatted(estoque.getUnidade().getId(), estoque.getProduto().getId());
    }

    private long cadastrar(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Outro Cliente","email":"%s","senha":"%s"}
                                """.formatted(email, SENHA)))
                .andExpect(status().isCreated())
                .andReturn();
        return numero(resultado.getResponse().getContentAsString(), "$.id");
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
