package br.edu.uninter.raizes.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uninter.raizes.domain.entidade.Estoque;
import br.edu.uninter.raizes.domain.entidade.Produto;
import br.edu.uninter.raizes.domain.entidade.Unidade;
import br.edu.uninter.raizes.infrastructure.persistencia.EstoqueRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.ProdutoRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UnidadeRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConsultaPedidoTest {

    private static final String SENHA = "Raizes@2026";
    private static final String CLIENTE = "cliente@raizes.test";
    private static final String ATENDENTE = "atendente@raizes.test";
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

    @Test
    void listaPedidosSemFiltroNaOrdemDaCriacao() throws Exception {
        long primeiro = criar(CLIENTE, "APP");
        long segundo = criar(CLIENTE, "TOTEM");

        String corpo = listar(token(CLIENTE), null, null, null, null);

        assertEquals(200, ultimoStatus);
        assertEquals(2, numero(corpo, "$.itens.length()"));
        assertEquals(segundo, numero(corpo, "$.itens[0].id"));
        assertEquals(primeiro, numero(corpo, "$.itens[1].id"));
        assertEquals("TOTEM", JsonPath.read(corpo, "$.itens[0].canalPedido"));
        assertEquals("AGUARDANDO_PAGAMENTO", JsonPath.read(corpo, "$.itens[0].status"));
        assertEquals(1, numero(corpo, "$.paginacao.pagina"));
        assertEquals(10, numero(corpo, "$.paginacao.limite"));
        assertEquals(2, numero(corpo, "$.paginacao.totalItens"));
    }

    @Test
    void filtraPorCanal() throws Exception {
        long app = criar(CLIENTE, "APP");
        criar(CLIENTE, "WEB");

        String corpo = listar(token(CLIENTE), "APP", null, null, null);

        assertEquals(List.of(app), ids(corpo));
    }

    @Test
    void filtraPorStatus() throws Exception {
        long pedido = criar(CLIENTE, "BALCAO");

        String corpo = listar(token(CLIENTE), null, "AGUARDANDO_PAGAMENTO", null, null);

        assertEquals(List.of(pedido), ids(corpo));
    }

    @Test
    void filtraPorCanalEStatus() throws Exception {
        long app = criar(CLIENTE, "APP");
        criar(CLIENTE, "TOTEM");

        String corpo = listar(token(CLIENTE), "APP", "AGUARDANDO_PAGAMENTO", null, null);

        assertEquals(List.of(app), ids(corpo));
        assertEquals("APP", JsonPath.read(corpo, "$.itens[0].canalPedido"));
    }

    @Test
    void retornaListaVaziaQuandoNaoHaCorrespondencia() throws Exception {
        criar(CLIENTE, "APP");

        String corpo = listar(token(CLIENTE), "PICKUP", null, null, null);

        assertEquals(200, ultimoStatus);
        assertEquals(0, numero(corpo, "$.itens.length()"));
        assertEquals(0, numero(corpo, "$.paginacao.totalItens"));
        assertEquals(0, numero(corpo, "$.paginacao.totalPaginas"));
    }

    @Test
    void rejeitaFiltroOuPaginacaoInvalida() throws Exception {
        String token = token(CLIENTE);

        esperarValidacao(token, "LOJA", null, null, null, "canalPedido");
        esperarValidacao(token, null, "DEVOLVIDO", null, null, "status");
        esperarValidacao(token, null, null, "0", null, "page");
        esperarValidacao(token, null, null, null, "51", "limit");
    }

    @Test
    void paginaAPartirDeUm() throws Exception {
        long primeiro = criar(CLIENTE, "APP");
        long segundo = criar(CLIENTE, "APP");
        long terceiro = criar(CLIENTE, "APP");

        String pagina1 = listar(token(CLIENTE), null, null, "1", "2");
        String pagina2 = listar(token(CLIENTE), null, null, "2", "2");

        assertEquals(List.of(terceiro, segundo), ids(pagina1));
        assertEquals(2, numero(pagina1, "$.paginacao.totalPaginas"));
        assertEquals(3, numero(pagina1, "$.paginacao.totalItens"));
        assertEquals(List.of(primeiro), ids(pagina2));
        assertEquals(2, numero(pagina2, "$.paginacao.pagina"));
    }

    @Test
    void clienteNaoVePedidoDeOutroCliente() throws Exception {
        long outroId = cadastrarCliente("outro.pedido@raizes.test");
        long meuPedido = criar(CLIENTE, "APP");
        long pedidoAlheio = criarPara(CLIENTE, outroId, "APP");

        String doCliente = listar(token(CLIENTE), null, null, null, null);
        String doAtendente = listar(token(ATENDENTE), null, null, null, null);

        assertEquals(List.of(meuPedido), ids(doCliente));
        assertTrue(ids(doAtendente).contains(meuPedido));
        assertTrue(ids(doAtendente).contains(pedidoAlheio));
        assertEquals(outroId, numero(doAtendente, "$.itens[?(@.id == " + pedidoAlheio + ")].clienteId"));
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/pedidos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NAO_AUTENTICADO"));
    }

    private int ultimoStatus;

    private long criar(String email, String canal) throws Exception {
        return criarPara(email, usuarios.findByEmail(email).orElseThrow().getId(), canal);
    }

    private long criarPara(String email, long clienteId, String canal) throws Exception {
        Estoque estoque = estoqueDe();
        MvcResult resultado = mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + token(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unidadeId":%d,"clienteId":%d,"canalPedido":"%s","itens":[{"produtoId":%d,"quantidade":1}]}
                                """.formatted(estoque.getUnidade().getId(), clienteId, canal, estoque.getProduto().getId())))
                .andExpect(status().isCreated())
                .andReturn();
        return numero(resultado.getResponse().getContentAsString(), "$.id");
    }

    private long cadastrarCliente(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Outro Cliente","email":"%s","senha":"%s"}
                                """.formatted(email, SENHA)))
                .andExpect(status().isCreated())
                .andReturn();
        return numero(resultado.getResponse().getContentAsString(), "$.id");
    }

    private String listar(String token, String canal, String status, String page, String limit) throws Exception {
        MockHttpServletRequestBuilder pedido = get("/pedidos").header("Authorization", "Bearer " + token);
        if (canal != null) {
            pedido.param("canalPedido", canal);
        }
        if (status != null) {
            pedido.param("status", status);
        }
        if (page != null) {
            pedido.param("page", page);
        }
        if (limit != null) {
            pedido.param("limit", limit);
        }
        MvcResult resultado = mockMvc.perform(pedido).andReturn();
        ultimoStatus = resultado.getResponse().getStatus();
        return resultado.getResponse().getContentAsString();
    }

    private void esperarValidacao(String token, String canal, String status, String page, String limit, String campo)
            throws Exception {
        String corpo = listar(token, canal, status, page, limit);
        assertEquals(422, ultimoStatus);
        assertEquals("VALIDACAO", JsonPath.read(corpo, "$.error"));
        assertEquals(campo, JsonPath.read(corpo, "$.details[0].field"));
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

    private Estoque estoqueDe() {
        Unidade unidade = unidades.findAll().stream()
                .filter(item -> item.getNome().equals(MATRIZ))
                .findFirst()
                .orElseThrow();
        Produto produto = produtos.findAll().stream()
                .filter(item -> item.getNome().equals(BAIAO))
                .findFirst()
                .orElseThrow();
        return estoques.findByUnidadeIdAndProdutoId(unidade.getId(), produto.getId()).orElseThrow();
    }

    private List<Long> ids(String corpo) {
        List<Number> valores = JsonPath.read(corpo, "$.itens[*].id");
        return valores.stream().map(Number::longValue).toList();
    }

    private long numero(String corpo, String caminho) {
        Object valor = JsonPath.read(corpo, caminho);
        if (valor instanceof List<?> lista) {
            return ((Number) lista.get(0)).longValue();
        }
        return ((Number) valor).longValue();
    }
}
