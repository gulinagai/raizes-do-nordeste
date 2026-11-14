package br.edu.uninter.raizes.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uninter.raizes.domain.entidade.Estoque;
import br.edu.uninter.raizes.domain.entidade.Produto;
import br.edu.uninter.raizes.domain.entidade.Unidade;
import br.edu.uninter.raizes.infrastructure.persistencia.EstoqueRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.PedidoRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.ProdutoRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UnidadeRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Instant;
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
class CriacaoPedidoTest {

    private static final String EMAIL = "cliente@raizes.test";
    private static final String SENHA = "Raizes@2026";
    private static final String MATRIZ = "Raízes Matriz Recife";
    private static final String FILIAL = "Raízes Filial Olinda";
    private static final String BAIAO = "Baião de dois";
    private static final String TAPIOCA = "Tapioca de queijo coalho";

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
    void criaPedidoComUmItemEReservaEstoque() throws Exception {
        Estoque estoque = estoqueDe(MATRIZ, BAIAO);
        estoque.setQuantidadeReservada(5);
        estoques.saveAndFlush(estoque);
        long clienteId = clienteId();
        long antes = pedidos.count();

        MvcResult resultado = postar(corpo(estoque.getUnidade().getId(), clienteId, "TOTEM", item(estoque.getProduto().getId(), 3)));

        assertEquals(201, resultado.getResponse().getStatus());
        String corpo = resultado.getResponse().getContentAsString();
        assertEquals("AGUARDANDO_PAGAMENTO", JsonPath.read(corpo, "$.status"));
        assertEquals("TOTEM", JsonPath.read(corpo, "$.canalPedido"));
        assertEquals(estoque.getUnidade().getId(), numero(corpo, "$.unidadeId"));
        assertEquals(clienteId, numero(corpo, "$.clienteId"));
        assertEquals(3, numero(corpo, "$.itens[0].quantidade"));
        assertEquals(0, new BigDecimal("34.90").compareTo(decimal(corpo, "$.itens[0].precoUnitario")));
        assertEquals(0, new BigDecimal("104.70").compareTo(decimal(corpo, "$.total")));
        assertNotNull(Instant.parse(JsonPath.read(corpo, "$.dataCriacao")));
        assertEquals(antes + 1, pedidos.count());

        Estoque atualizado = estoques.findByUnidadeIdAndProdutoId(estoque.getUnidade().getId(), estoque.getProduto().getId()).orElseThrow();
        assertEquals(40, atualizado.getQuantidadeTotal());
        assertEquals(8, atualizado.getQuantidadeReservada());
        assertEquals(32, atualizado.quantidadeDisponivel());
        assertEquals(18, estoqueDe(FILIAL, BAIAO).getQuantidadeTotal());
        assertEquals(0, estoqueDe(FILIAL, BAIAO).getQuantidadeReservada());
    }

    @Test
    void criaPedidoComVariosItens() throws Exception {
        Estoque baiao = estoqueDe(MATRIZ, BAIAO);
        Estoque tapioca = estoqueDe(MATRIZ, TAPIOCA);

        MvcResult resultado = postar(corpo(
                baiao.getUnidade().getId(),
                clienteId(),
                "APP",
                item(baiao.getProduto().getId(), 1) + "," + item(tapioca.getProduto().getId(), 2)));

        assertEquals(201, resultado.getResponse().getStatus());
        String corpo = resultado.getResponse().getContentAsString();
        assertEquals(2, numero(corpo, "$.itens.length()"));
        assertEquals(0, new BigDecimal("62.90").compareTo(decimal(corpo, "$.total")));
        assertEquals(1, estoques.findByUnidadeIdAndProdutoId(baiao.getUnidade().getId(), baiao.getProduto().getId()).orElseThrow().getQuantidadeReservada());
        assertEquals(2, estoques.findByUnidadeIdAndProdutoId(tapioca.getUnidade().getId(), tapioca.getProduto().getId()).orElseThrow().getQuantidadeReservada());
        assertEquals(40, baiao.getQuantidadeTotal());
        assertEquals(50, tapioca.getQuantidadeTotal());
    }

    @Test
    void rejeitaQuantidadeAcimaDoDisponivelSemReservaParcial() throws Exception {
        Estoque baiao = estoqueDe(MATRIZ, BAIAO);
        Estoque tapioca = estoqueDe(MATRIZ, TAPIOCA);
        long antes = pedidos.count();

        MvcResult resultado = postar(corpo(
                baiao.getUnidade().getId(),
                clienteId(),
                "BALCAO",
                item(tapioca.getProduto().getId(), 1) + "," + item(baiao.getProduto().getId(), 41)));

        assertEquals(409, resultado.getResponse().getStatus());
        String corpo = resultado.getResponse().getContentAsString();
        assertEquals("ESTOQUE_INSUFICIENTE", JsonPath.read(corpo, "$.error"));
        assertEquals("Não há quantidade suficiente para um ou mais itens.", JsonPath.read(corpo, "$.message"));
        assertEquals("itens[1].quantidade", JsonPath.read(corpo, "$.details[0].field"));
        assertEquals("Disponível: 40", JsonPath.read(corpo, "$.details[0].issue"));
        assertEquals(antes, pedidos.count());
        assertEquals(0, estoqueDe(MATRIZ, BAIAO).getQuantidadeReservada());
        assertEquals(0, estoqueDe(MATRIZ, TAPIOCA).getQuantidadeReservada());
        assertEquals(40, baiao.getQuantidadeTotal());
        assertEquals(50, tapioca.getQuantidadeTotal());
    }

    @Test
    void somaItensRepetidosNaMesmaReserva() throws Exception {
        Estoque estoque = estoqueDe(MATRIZ, BAIAO);
        estoque.setQuantidadeReservada(5);
        estoques.saveAndFlush(estoque);
        long produtoId = estoque.getProduto().getId();

        MvcResult resultado = postar(corpo(
                estoque.getUnidade().getId(),
                clienteId(),
                "TOTEM",
                item(produtoId, 10) + "," + item(produtoId, 6)));

        assertEquals(201, resultado.getResponse().getStatus());
        Estoque atualizado = estoques.findByUnidadeIdAndProdutoId(estoque.getUnidade().getId(), produtoId).orElseThrow();
        assertEquals(40, atualizado.getQuantidadeTotal());
        assertEquals(21, atualizado.getQuantidadeReservada());
        assertEquals(19, atualizado.quantidadeDisponivel());
    }

    @Test
    void rejeitaSomaDeItensRepetidosAcimaDoDisponivel() throws Exception {
        Estoque estoque = estoqueDe(MATRIZ, BAIAO);
        long antes = pedidos.count();

        MvcResult resultado = postar(corpo(
                estoque.getUnidade().getId(),
                clienteId(),
                "APP",
                item(estoque.getProduto().getId(), 30) + "," + item(estoque.getProduto().getId(), 15)));

        assertEquals(409, resultado.getResponse().getStatus());
        String corpo = resultado.getResponse().getContentAsString();
        assertEquals("itens[1].quantidade", JsonPath.read(corpo, "$.details[0].field"));
        assertEquals("Disponível: 10", JsonPath.read(corpo, "$.details[0].issue"));
        assertEquals(antes, pedidos.count());
        assertEquals(40, estoque.getQuantidadeTotal());
        assertEquals(0, estoque.getQuantidadeReservada());
    }

    @Test
    void rejeitaProdutoSemEstoqueNaUnidade() throws Exception {
        Produto semEstoque = produtos.saveAndFlush(
                new Produto("Produto Sem Saldo Pedido", "Sem registro na unidade.", new BigDecimal("9.00"), true));
        Estoque baiao = estoqueDe(MATRIZ, BAIAO);
        long antes = pedidos.count();

        MvcResult resultado = postar(corpo(baiao.getUnidade().getId(), clienteId(), "WEB", item(semEstoque.getId(), 1)));

        assertEquals(409, resultado.getResponse().getStatus());
        String corpo = resultado.getResponse().getContentAsString();
        assertEquals("ESTOQUE_INSUFICIENTE", JsonPath.read(corpo, "$.error"));
        assertEquals("Disponível: 0", JsonPath.read(corpo, "$.details[0].issue"));
        assertEquals(antes, pedidos.count());
        assertEquals(0, baiao.getQuantidadeReservada());
        assertEquals(18, estoqueDe(FILIAL, BAIAO).getQuantidadeTotal());
    }

    @Test
    void rejeitaQuantidadeInvalida() throws Exception {
        Estoque estoque = estoqueDe(MATRIZ, BAIAO);
        long antes = pedidos.count();

        mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(estoque.getUnidade().getId(), clienteId(), "WEB", item(estoque.getProduto().getId(), 0))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("VALIDACAO"))
                .andExpect(jsonPath("$.details[0].field").value("itens[0].quantidade"))
                .andExpect(jsonPath("$.details[0].issue").value("deve ser maior que zero"));

        assertEquals(antes, pedidos.count());
        assertEquals(0, estoque.getQuantidadeReservada());
    }

    @Test
    void rejeitaUnidadeClienteOuProdutoInexistente() throws Exception {
        Estoque estoque = estoqueDe(MATRIZ, BAIAO);
        long clienteId = clienteId();
        long antes = pedidos.count();

        esperarNaoEncontrado(corpo(999_999_999L, clienteId, "PICKUP", item(estoque.getProduto().getId(), 1)));
        esperarNaoEncontrado(corpo(estoque.getUnidade().getId(), 999_999_999L, "PICKUP", item(estoque.getProduto().getId(), 1)));
        esperarNaoEncontrado(corpo(estoque.getUnidade().getId(), clienteId, "PICKUP", item(999_999_999L, 1)));

        assertEquals(antes, pedidos.count());
        assertEquals(0, estoque.getQuantidadeReservada());
    }

    @Test
    void rejeitaUnidadeOuProdutoInativo() throws Exception {
        Unidade matriz = unidade(MATRIZ);
        Produto baiao = produto(BAIAO);
        long antes = pedidos.count();
        matriz.setAtivo(false);

        MvcResult unidadeInativa = postar(corpo(matriz.getId(), clienteId(), "WEB", item(baiao.getId(), 1)));

        assertEquals(409, unidadeInativa.getResponse().getStatus());
        assertEquals("RECURSO_INATIVO", JsonPath.read(unidadeInativa.getResponse().getContentAsString(), "$.error"));
        assertEquals("unidadeId", JsonPath.read(unidadeInativa.getResponse().getContentAsString(), "$.details[0].field"));

        matriz.setAtivo(true);
        baiao.setAtivo(false);
        MvcResult produtoInativo = postar(corpo(matriz.getId(), clienteId(), "WEB", item(baiao.getId(), 1)));

        assertEquals(409, produtoInativo.getResponse().getStatus());
        assertEquals("itens[0].produtoId", JsonPath.read(produtoInativo.getResponse().getContentAsString(), "$.details[0].field"));
        assertEquals(antes, pedidos.count());
        assertEquals(0, estoqueDe(MATRIZ, BAIAO).getQuantidadeReservada());
    }

    private void esperarNaoEncontrado(String json) throws Exception {
        mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NAO_ENCONTRADO"));
    }

    private MvcResult postar(String json) throws Exception {
        return mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn();
    }

    private String corpo(long unidadeId, long clienteId, String canal, String itens) {
        return """
                {"unidadeId":%d,"clienteId":%d,"canalPedido":"%s","itens":[%s]}
                """.formatted(unidadeId, clienteId, canal, itens);
    }

    private String item(long produtoId, int quantidade) {
        return """
                {"produtoId":%d,"quantidade":%d}
                """.formatted(produtoId, quantidade);
    }

    private long clienteId() {
        return usuarios.findByEmail(EMAIL).orElseThrow().getId();
    }

    private String token() throws Exception {
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(EMAIL, SENHA)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    private Unidade unidade(String nome) {
        return unidades.findAll().stream().filter(unidade -> unidade.getNome().equals(nome)).findFirst().orElseThrow();
    }

    private Produto produto(String nome) {
        return produtos.findAll().stream().filter(produto -> produto.getNome().equals(nome)).findFirst().orElseThrow();
    }

    private Estoque estoqueDe(String unidadeNome, String produtoNome) {
        Unidade unidade = unidade(unidadeNome);
        Produto produto = produto(produtoNome);
        return estoques.findByUnidadeIdAndProdutoId(unidade.getId(), produto.getId()).orElseThrow();
    }

    private long numero(String corpo, String caminho) {
        return ((Number) JsonPath.read(corpo, caminho)).longValue();
    }

    private BigDecimal decimal(String corpo, String caminho) {
        Object valor = JsonPath.read(corpo, caminho);
        return new BigDecimal(valor.toString());
    }
}
