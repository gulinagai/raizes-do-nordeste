package br.edu.uninter.raizes.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uninter.raizes.domain.entidade.Estoque;
import br.edu.uninter.raizes.domain.entidade.MovimentacaoEstoque;
import br.edu.uninter.raizes.domain.entidade.Produto;
import br.edu.uninter.raizes.domain.entidade.Unidade;
import br.edu.uninter.raizes.infrastructure.persistencia.EstoqueRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.MovimentacaoEstoqueRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.ProdutoRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UnidadeRepositorio;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Instant;
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
class MovimentacaoEstoqueTest {

    private static final String EMAIL = "cliente@raizes.test";
    private static final String SENHA = "Raizes@2026";
    private static final String MATRIZ = "Raízes Matriz Recife";
    private static final String FILIAL = "Raízes Filial Olinda";
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
    private MovimentacaoEstoqueRepositorio movimentacoes;

    @Test
    void entradaAtualizaTotalEPreservaReserva() throws Exception {
        Estoque estoque = reservar(MATRIZ, BAIAO, 5);
        long unidadeId = estoque.getUnidade().getId();
        long produtoId = estoque.getProduto().getId();

        MvcResult resultado = postar(corpo(unidadeId, produtoId, "ENTRADA", 10));

        assertEquals(201, resultado.getResponse().getStatus());
        assertEquals(50, json(resultado, "$.quantidadeTotal"));
        assertEquals(5, json(resultado, "$.quantidadeReservada"));
        assertEquals(45, json(resultado, "$.quantidadeDisponivel"));
        assertEquals(unidadeId, json(resultado, "$.unidadeId"));
        assertEquals(produtoId, json(resultado, "$.produtoId"));
        assertNotNull(Instant.parse(JsonPath.read(resultado.getResponse().getContentAsString(), "$.dataHora")));

        Estoque atualizado = estoques.findByUnidadeIdAndProdutoId(unidadeId, produtoId).orElseThrow();
        assertEquals(50, atualizado.getQuantidadeTotal());
        assertEquals(5, atualizado.getQuantidadeReservada());
        assertEquals(45, atualizado.quantidadeDisponivel());

        long id = json(resultado, "$.id");
        MovimentacaoEstoque movimentacao = movimentacoes.findById(id).orElseThrow();
        assertEquals("Reposição de estoque", movimentacao.getObservacao());
        assertEquals(18, estoqueDe(FILIAL, BAIAO).getQuantidadeTotal());
    }

    @Test
    void entradaSemRegistroInicializaEstoque() throws Exception {
        Unidade matriz = unidade(MATRIZ);
        Produto produto = produtos.saveAndFlush(
                new Produto("Produto Novo Estoque", "Ainda sem saldo.", new BigDecimal("9.00"), true));
        assertTrue(estoques.findByUnidadeIdAndProdutoId(matriz.getId(), produto.getId()).isEmpty());

        MvcResult resultado = postar(corpo(matriz.getId(), produto.getId(), "ENTRADA", 7));

        assertEquals(201, resultado.getResponse().getStatus());
        assertEquals(7, json(resultado, "$.quantidadeTotal"));
        assertEquals(0, json(resultado, "$.quantidadeReservada"));
        assertEquals(7, json(resultado, "$.quantidadeDisponivel"));
        Estoque criado = estoques.findByUnidadeIdAndProdutoId(matriz.getId(), produto.getId()).orElseThrow();
        assertEquals(7, criado.getQuantidadeTotal());
        assertEquals(0, criado.getQuantidadeReservada());
    }

    @Test
    void saidaAtualizaTotalEPreservaReserva() throws Exception {
        Estoque estoque = reservar(MATRIZ, BAIAO, 10);
        long unidadeId = estoque.getUnidade().getId();
        long produtoId = estoque.getProduto().getId();

        MvcResult resultado = postar(corpo(unidadeId, produtoId, "SAIDA", 20));

        assertEquals(201, resultado.getResponse().getStatus());
        assertEquals("SAIDA", JsonPath.read(resultado.getResponse().getContentAsString(), "$.tipo"));
        assertEquals(20, json(resultado, "$.quantidadeTotal"));
        assertEquals(10, json(resultado, "$.quantidadeReservada"));
        assertEquals(10, json(resultado, "$.quantidadeDisponivel"));

        Estoque atualizado = estoques.findByUnidadeIdAndProdutoId(unidadeId, produtoId).orElseThrow();
        assertEquals(20, atualizado.getQuantidadeTotal());
        assertEquals(10, atualizado.getQuantidadeReservada());
        assertEquals(18, estoqueDe(FILIAL, BAIAO).getQuantidadeTotal());
    }

    @Test
    void rejeitaSaidaAcimaDoDisponivel() throws Exception {
        Estoque estoque = reservar(MATRIZ, BAIAO, 0);
        long movimentos = movimentacoes.count();

        MvcResult resultado = postar(corpo(estoque.getUnidade().getId(), estoque.getProduto().getId(), "SAIDA", 41));

        assertEquals(409, resultado.getResponse().getStatus());
        assertEquals("ESTOQUE_INSUFICIENTE", JsonPath.read(resultado.getResponse().getContentAsString(), "$.error"));
        assertInalterado(estoque.getUnidade().getId(), estoque.getProduto().getId(), 40, 0, movimentos);
    }

    @Test
    void rejeitaSaidaQueConsumiriaReserva() throws Exception {
        Estoque estoque = reservar(MATRIZ, BAIAO, 10);
        long movimentos = movimentacoes.count();

        MvcResult resultado = postar(corpo(estoque.getUnidade().getId(), estoque.getProduto().getId(), "SAIDA", 35));

        assertEquals(409, resultado.getResponse().getStatus());
        assertEquals("ESTOQUE_INSUFICIENTE", JsonPath.read(resultado.getResponse().getContentAsString(), "$.error"));
        assertEquals("Disponível: 30", JsonPath.read(resultado.getResponse().getContentAsString(), "$.details[0].issue"));
        assertInalterado(estoque.getUnidade().getId(), estoque.getProduto().getId(), 40, 10, movimentos);
        assertEquals(18, estoqueDe(FILIAL, BAIAO).getQuantidadeTotal());
    }

    @Test
    void rejeitaSaidaSemEstoque() throws Exception {
        Unidade matriz = unidade(MATRIZ);
        Produto produto = produtos.saveAndFlush(
                new Produto("Produto Sem Saldo", "Sem registro.", new BigDecimal("8.00"), true));
        long movimentos = movimentacoes.count();

        MvcResult resultado = postar(corpo(matriz.getId(), produto.getId(), "SAIDA", 1));

        assertEquals(409, resultado.getResponse().getStatus());
        assertEquals("ESTOQUE_INSUFICIENTE", JsonPath.read(resultado.getResponse().getContentAsString(), "$.error"));
        assertTrue(estoques.findByUnidadeIdAndProdutoId(matriz.getId(), produto.getId()).isEmpty());
        assertEquals(movimentos, movimentacoes.count());
    }

    @Test
    void rejeitaQuantidadeZeroOuNegativa() throws Exception {
        Estoque estoque = estoqueDe(MATRIZ, BAIAO);
        long movimentos = movimentacoes.count();

        esperarValidacao(estoque, 0, "deve ser maior que zero");
        esperarValidacao(estoque, -3, "deve ser maior que zero");
        assertInalterado(estoque.getUnidade().getId(), estoque.getProduto().getId(), 40, 0, movimentos);
    }

    @Test
    void rejeitaUnidadeInexistente() throws Exception {
        long produtoId = produto(BAIAO).getId();
        long movimentos = movimentacoes.count();

        MvcResult resultado = postar(corpo(999_999_999L, produtoId, "ENTRADA", 1));

        assertEquals(404, resultado.getResponse().getStatus());
        assertEquals("NAO_ENCONTRADO", JsonPath.read(resultado.getResponse().getContentAsString(), "$.error"));
        assertEquals(movimentos, movimentacoes.count());
        assertEquals(40, estoqueDe(MATRIZ, BAIAO).getQuantidadeTotal());
    }

    @Test
    void rejeitaProdutoInexistente() throws Exception {
        long unidadeId = unidade(MATRIZ).getId();
        long movimentos = movimentacoes.count();

        MvcResult resultado = postar(corpo(unidadeId, 999_999_999L, "SAIDA", 1));

        assertEquals(404, resultado.getResponse().getStatus());
        assertEquals("NAO_ENCONTRADO", JsonPath.read(resultado.getResponse().getContentAsString(), "$.error"));
        assertEquals(movimentos, movimentacoes.count());
        assertEquals(40, estoqueDe(MATRIZ, BAIAO).getQuantidadeTotal());
    }

    @Test
    void rejeitaTipoInvalido() throws Exception {
        Estoque estoque = estoqueDe(MATRIZ, BAIAO);
        long movimentos = movimentacoes.count();
        String json = """
                {"unidadeId":%d,"produtoId":%d,"tipo":"AJUSTE","quantidade":1}
                """.formatted(estoque.getUnidade().getId(), estoque.getProduto().getId());

        MvcResult resultado = postar(json);

        assertEquals(422, resultado.getResponse().getStatus());
        assertEquals("VALIDACAO", JsonPath.read(resultado.getResponse().getContentAsString(), "$.error"));
        assertInalterado(estoque.getUnidade().getId(), estoque.getProduto().getId(), 40, 0, movimentos);
    }

    private void esperarValidacao(Estoque estoque, int quantidade, String mensagem) throws Exception {
        MvcResult resultado = postar(corpo(estoque.getUnidade().getId(), estoque.getProduto().getId(), "SAIDA", quantidade));
        assertEquals(422, resultado.getResponse().getStatus());
        assertEquals("VALIDACAO", JsonPath.read(resultado.getResponse().getContentAsString(), "$.error"));
        List<String> mensagens = JsonPath.read(
                resultado.getResponse().getContentAsString(), "$.details[?(@.field == 'quantidade')].issue");
        assertEquals(List.of(mensagem), mensagens);
    }

    private void assertInalterado(long unidadeId, long produtoId, int total, int reservada, long movimentos) {
        Estoque atual = estoques.findByUnidadeIdAndProdutoId(unidadeId, produtoId).orElseThrow();
        assertEquals(total, atual.getQuantidadeTotal());
        assertEquals(reservada, atual.getQuantidadeReservada());
        assertEquals(movimentos, movimentacoes.count());
    }

    private Estoque reservar(String unidadeNome, String produtoNome, int reservada) {
        Estoque estoque = estoqueDe(unidadeNome, produtoNome);
        estoque.setQuantidadeReservada(reservada);
        return estoques.saveAndFlush(estoque);
    }

    private MvcResult postar(String json) throws Exception {
        return mockMvc.perform(post("/estoque/movimentacoes")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn();
    }

    private String corpo(long unidadeId, long produtoId, String tipo, int quantidade) {
        return """
                {"unidadeId":%d,"produtoId":%d,"tipo":"%s","quantidade":%d,"observacao":"Reposição de estoque"}
                """.formatted(unidadeId, produtoId, tipo, quantidade);
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

    private int json(MvcResult resultado, String caminho) throws Exception {
        return ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), caminho)).intValue();
    }
}
