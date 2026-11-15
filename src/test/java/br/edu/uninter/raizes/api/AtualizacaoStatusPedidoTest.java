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
class AtualizacaoStatusPedidoTest {

    private static final String EMAIL = "cliente@raizes.test";
    private static final String COZINHA = "cozinha@raizes.test";
    private static final String ATENDENTE = "atendente@raizes.test";
    private static final String SENHA = "Raizes@2026";
    private static final String MATRIZ = "Raízes Matriz Recife";
    private static final String BAIAO = "Baião de dois";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UnidadeRepositorio unidades;

    @Autowired
    private ProdutoRepositorio produtos;

    @Autowired
    private EstoqueRepositorio estoques;

    @Autowired
    private UsuarioRepositorio usuarios;

    @Autowired
    private PedidoRepositorio pedidos;

    @Test
    void marcaProntoQuandoEstaEmPreparo() throws Exception {
        long pedidoId = pedidoEmPreparo();
        int total = estoque().getQuantidadeTotal();
        int reservada = estoque().getQuantidadeReservada();

        MvcResult resultado = atualizar(pedidoId, "PRONTO", token(COZINHA));

        assertEquals(200, resultado.getResponse().getStatus());
        assertEquals("PRONTO", JsonPath.read(resultado.getResponse().getContentAsString(), "$.status"));
        assertEquals(StatusPedido.PRONTO, pedidos.findById(pedidoId).orElseThrow().getStatus());
        assertEquals(total, estoque().getQuantidadeTotal());
        assertEquals(reservada, estoque().getQuantidadeReservada());

        MvcResult repetido = atualizar(pedidoId, "PRONTO", token(COZINHA));
        assertEquals(409, repetido.getResponse().getStatus());
        assertEquals("TRANSICAO_NAO_PERMITIDA", JsonPath.read(repetido.getResponse().getContentAsString(), "$.error"));
        assertEquals(StatusPedido.PRONTO, pedidos.findById(pedidoId).orElseThrow().getStatus());
    }

    @Test
    void marcaEntregueQuandoEstaPronto() throws Exception {
        long pedidoId = pedidoEmPreparo();
        assertEquals(200, atualizar(pedidoId, "PRONTO", token(COZINHA)).getResponse().getStatus());

        MvcResult resultado = atualizar(pedidoId, "ENTREGUE", token(ATENDENTE));

        assertEquals(200, resultado.getResponse().getStatus());
        assertEquals("ENTREGUE", JsonPath.read(resultado.getResponse().getContentAsString(), "$.status"));
        assertEquals(StatusPedido.ENTREGUE, pedidos.findById(pedidoId).orElseThrow().getStatus());

        assertEquals(409, atualizar(pedidoId, "PRONTO", token(COZINHA)).getResponse().getStatus());
        assertEquals(409, atualizar(pedidoId, "ENTREGUE", token(ATENDENTE)).getResponse().getStatus());
        assertEquals(StatusPedido.ENTREGUE, pedidos.findById(pedidoId).orElseThrow().getStatus());
    }

    @Test
    void rejeitaTransicaoForaDaSequencia() throws Exception {
        long aguardando = pedidoAguardando();
        long emPreparo = pedidoEmPreparo();

        assertEquals(409, atualizar(aguardando, "PRONTO", token(COZINHA)).getResponse().getStatus());
        assertEquals(409, atualizar(aguardando, "ENTREGUE", token(ATENDENTE)).getResponse().getStatus());
        assertEquals(409, atualizar(emPreparo, "ENTREGUE", token(ATENDENTE)).getResponse().getStatus());
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, pedidos.findById(aguardando).orElseThrow().getStatus());
        assertEquals(StatusPedido.EM_PREPARO, pedidos.findById(emPreparo).orElseThrow().getStatus());
    }

    @Test
    void rejeitaPedidoInexistenteStatusInvalidoESemAutenticacao() throws Exception {
        long pedidoId = pedidoEmPreparo();
        String token = token(COZINHA);

        mockMvc.perform(patch("/pedidos/{id}/status", 999999999L)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PRONTO"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NAO_ENCONTRADO"));

        mockMvc.perform(patch("/pedidos/{id}/status", pedidoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DEVOLVIDO"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("VALIDACAO"));

        mockMvc.perform(patch("/pedidos/{id}/status", pedidoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("VALIDACAO"))
                .andExpect(jsonPath("$.details[0].field").value("status"));

        mockMvc.perform(patch("/pedidos/{id}/status", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PRONTO"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NAO_AUTENTICADO"));

        assertEquals(StatusPedido.EM_PREPARO, pedidos.findById(pedidoId).orElseThrow().getStatus());
    }

    private long pedidoEmPreparo() throws Exception {
        long pedidoId = pedidoAguardando();
        mockMvc.perform(post("/pagamentos")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId":%d,"resultadoSimulado":"APROVADO"}
                                """.formatted(pedidoId)))
                .andExpect(status().isCreated());
        return pedidoId;
    }

    private long pedidoAguardando() throws Exception {
        Estoque estoque = estoque();
        long clienteId = usuarios.findByEmail(EMAIL).orElseThrow().getId();
        MvcResult pedido = mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unidadeId":%d,"clienteId":%d,"canalPedido":"BALCAO","itens":[{"produtoId":%d,"quantidade":1}]}
                                """.formatted(estoque.getUnidade().getId(), clienteId, estoque.getProduto().getId())))
                .andExpect(status().isCreated())
                .andReturn();
        return numero(pedido.getResponse().getContentAsString(), "$.id");
    }

    private MvcResult atualizar(long pedidoId, String statusPedido, String token) throws Exception {
        return mockMvc.perform(patch("/pedidos/{id}/status", pedidoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"%s"}
                                """.formatted(statusPedido)))
                .andReturn();
    }

    private String token() throws Exception {
        return token(EMAIL);
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
        Unidade unidade = unidades.findAll().stream().filter(item -> item.getNome().equals(MATRIZ)).findFirst().orElseThrow();
        Produto produto = produtos.findAll().stream().filter(item -> item.getNome().equals(BAIAO)).findFirst().orElseThrow();
        return estoques.findByUnidadeIdAndProdutoId(unidade.getId(), produto.getId()).orElseThrow();
    }

    private long numero(String corpo, String caminho) {
        return ((Number) JsonPath.read(corpo, caminho)).longValue();
    }
}
