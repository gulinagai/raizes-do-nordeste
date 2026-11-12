package br.edu.uninter.raizes.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
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
class ConsultaCardapioTest {

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

    @Test
    void consultaCardapioDaUnidade() throws Exception {
        Unidade matriz = unidade(MATRIZ);
        String token = token();

        MvcResult resultado = mockMvc.perform(get("/unidades/{id}/cardapio", matriz.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidade.id").value(matriz.getId()))
                .andExpect(jsonPath("$.unidade.nome").value(MATRIZ))
                .andExpect(jsonPath("$.paginacao.pagina").value(1))
                .andExpect(jsonPath("$.paginacao.limite").value(10))
                .andExpect(jsonPath("$.paginacao.totalItens").value(produtosAtivos()))
                .andReturn();

        String corpo = resultado.getResponse().getContentAsString();
        assertEquals(40, quantidade(corpo, BAIAO));
        assertTrue(disponivel(corpo, BAIAO));
        assertEquals(34.90, preco(corpo, BAIAO), 0.001);
        assertEquals(30, quantidade(corpo, "Carne de sol com macaxeira"));
        assertEquals(50, quantidade(corpo, "Tapioca de queijo coalho"));
        List<String> nomes = JsonPath.read(corpo, "$.itens[*].nome");
        assertTrue(nomes.indexOf(BAIAO) < nomes.indexOf("Carne de sol com macaxeira"));
        assertTrue(nomes.indexOf("Carne de sol com macaxeira") < nomes.indexOf("Tapioca de queijo coalho"));
        assertFalse(corpo.contains("quantidadeTotal"));
        assertFalse(corpo.contains("quantidadeReservada"));

        Estoque estoque = estoques.findByUnidadeIdAndProdutoId(matriz.getId(), produtoId(corpo, BAIAO)).orElseThrow();
        assertEquals(40, estoque.getQuantidadeTotal());
        assertEquals(0, estoque.getQuantidadeReservada());
    }

    @Test
    void retornaSomenteProdutosAtivos() throws Exception {
        long ativos = produtosAtivos();
        produtos.saveAndFlush(new Produto("Produto Inativo Teste", "Não deve aparecer.", new BigDecimal("9.00"), false));

        String corpo = cardapio(unidade(MATRIZ).getId(), 50);

        assertFalse(nomes(corpo).contains("Produto Inativo Teste"));
        assertEquals(ativos, ((Number) JsonPath.read(corpo, "$.paginacao.totalItens")).longValue());
    }

    @Test
    void calculaQuantidadeDisponivelDescontandoReserva() throws Exception {
        Unidade matriz = unidade(MATRIZ);
        Produto produto = produtos.saveAndFlush(
                new Produto("Produto Reserva Parcial", "Com reserva.", new BigDecimal("10.00"), true));
        estoques.saveAndFlush(new Estoque(matriz, produto, 10, 3));

        String corpo = cardapio(matriz.getId(), 50);

        assertEquals(7, quantidade(corpo, "Produto Reserva Parcial"));
        assertTrue(disponivel(corpo, "Produto Reserva Parcial"));
    }

    @Test
    void estoqueTotalmenteReservadoFicaIndisponivel() throws Exception {
        Unidade matriz = unidade(MATRIZ);
        Produto produto = produtos.saveAndFlush(
                new Produto("Produto Reserva Total", "Tudo reservado.", new BigDecimal("11.00"), true));
        estoques.saveAndFlush(new Estoque(matriz, produto, 5, 5));

        String corpo = cardapio(matriz.getId(), 50);

        assertEquals(0, quantidade(corpo, "Produto Reserva Total"));
        assertFalse(disponivel(corpo, "Produto Reserva Total"));
    }

    @Test
    void produtoSemEstoqueApareceComQuantidadeZero() throws Exception {
        produtos.saveAndFlush(new Produto("Produto Sem Estoque", "Sem saldo na unidade.", new BigDecimal("8.00"), true));

        String corpo = cardapio(unidade(MATRIZ).getId(), 50);

        assertEquals(0, quantidade(corpo, "Produto Sem Estoque"));
        assertFalse(disponivel(corpo, "Produto Sem Estoque"));
    }

    @Test
    void unidadeInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/unidades/{id}/cardapio", 999_999_999L).header("Authorization", "Bearer " + token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.message").value("Recurso não encontrado."))
                .andExpect(jsonPath("$.path").value("/unidades/999999999/cardapio"));
    }

    @Test
    void unidadeSemProdutosAtivosRetornaListaVazia() throws Exception {
        Unidade matriz = unidade(MATRIZ);
        produtos.findAll().forEach(produto -> produto.setAtivo(false));
        produtos.flush();

        mockMvc.perform(get("/unidades/{id}/cardapio", matriz.getId()).header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidade.id").value(matriz.getId()))
                .andExpect(jsonPath("$.itens").isEmpty())
                .andExpect(jsonPath("$.paginacao.pagina").value(1))
                .andExpect(jsonPath("$.paginacao.limite").value(10))
                .andExpect(jsonPath("$.paginacao.totalItens").value(0))
                .andExpect(jsonPath("$.paginacao.totalPaginas").value(0));
    }

    @Test
    void paginacaoComecaEmUm() throws Exception {
        long total = produtosAtivos() + 3;
        criarProdutosOrdenaveis();

        mockMvc.perform(get("/unidades/{id}/cardapio", unidade(MATRIZ).getId())
                        .header("Authorization", "Bearer " + token())
                        .param("page", "1")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].nome").value("0001 Cardapio"))
                .andExpect(jsonPath("$.itens[1].nome").value("0002 Cardapio"))
                .andExpect(jsonPath("$.paginacao.pagina").value(1))
                .andExpect(jsonPath("$.paginacao.limite").value(2))
                .andExpect(jsonPath("$.paginacao.totalItens").value(total))
                .andExpect(jsonPath("$.paginacao.totalPaginas").value(paginas(total, 2)));
    }

    @Test
    void consultaSegundaPagina() throws Exception {
        long total = produtosAtivos() + 3;
        criarProdutosOrdenaveis();

        mockMvc.perform(get("/unidades/{id}/cardapio", unidade(MATRIZ).getId())
                        .header("Authorization", "Bearer " + token())
                        .param("page", "2")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value((int) Math.min(2, total - 2)))
                .andExpect(jsonPath("$.itens[0].nome").value("0003 Cardapio"))
                .andExpect(jsonPath("$.paginacao.pagina").value(2))
                .andExpect(jsonPath("$.paginacao.limite").value(2))
                .andExpect(jsonPath("$.paginacao.totalItens").value(total))
                .andExpect(jsonPath("$.paginacao.totalPaginas").value(paginas(total, 2)));
    }

    @Test
    void paginaSemResultadosRetornaListaVazia() throws Exception {
        long total = produtosAtivos();
        int paginaVazia = paginas(total, 10) + 1;

        mockMvc.perform(get("/unidades/{id}/cardapio", unidade(MATRIZ).getId())
                        .header("Authorization", "Bearer " + token())
                        .param("page", Integer.toString(paginaVazia))
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isEmpty())
                .andExpect(jsonPath("$.paginacao.pagina").value(paginaVazia))
                .andExpect(jsonPath("$.paginacao.limite").value(10))
                .andExpect(jsonPath("$.paginacao.totalItens").value(total))
                .andExpect(jsonPath("$.paginacao.totalPaginas").value(paginas(total, 10)));
    }

    @Test
    void rejeitaPaginacaoInvalida() throws Exception {
        long unidadeId = unidade(MATRIZ).getId();
        String token = token();

        esperarValidacao(unidadeId, token, "page", "0", "limit", "10", "page", "deve ser maior ou igual a 1");
        esperarValidacao(unidadeId, token, "page", "1", "limit", "0", "limit", "deve estar entre 1 e 50");
        esperarValidacao(unidadeId, token, "page", "1", "limit", "51", "limit", "deve estar entre 1 e 50");

        mockMvc.perform(get("/unidades/{id}/cardapio", unidadeId)
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("limit", "80"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.details[?(@.field == 'page')]").exists())
                .andExpect(jsonPath("$.details[?(@.field == 'limit')]").exists());

        mockMvc.perform(get("/unidades/{id}/cardapio", unidadeId)
                        .header("Authorization", "Bearer " + token)
                        .param("page", "abc"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("VALIDACAO"))
                .andExpect(jsonPath("$.details[?(@.field == 'page')].issue").value("deve ser um número inteiro"));
    }

    @Test
    void cardapioNaoUsaEstoqueDeOutraUnidade() throws Exception {
        String token = token();
        String matriz = cardapio(unidade(MATRIZ).getId(), 50, token);
        String filial = cardapio(unidade(FILIAL).getId(), 50, token);

        assertEquals(40, quantidade(matriz, BAIAO));
        assertEquals(18, quantidade(filial, BAIAO));
        assertEquals(30, quantidade(matriz, "Carne de sol com macaxeira"));
        assertEquals(12, quantidade(filial, "Carne de sol com macaxeira"));
    }

    private void esperarValidacao(
            long unidadeId, String token, String nome1, String valor1, String nome2, String valor2, String campo, String mensagem)
            throws Exception {
        mockMvc.perform(get("/unidades/{id}/cardapio", unidadeId)
                        .header("Authorization", "Bearer " + token)
                        .param(nome1, valor1)
                        .param(nome2, valor2))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("VALIDACAO"))
                .andExpect(jsonPath("$.message").value("Os dados informados são inválidos."))
                .andExpect(jsonPath("$.details[?(@.field == '" + campo + "')].issue").value(mensagem));
    }

    private void criarProdutosOrdenaveis() {
        produtos.saveAndFlush(new Produto("0001 Cardapio", "Primeiro.", new BigDecimal("1.00"), true));
        produtos.saveAndFlush(new Produto("0002 Cardapio", "Segundo.", new BigDecimal("2.00"), true));
        produtos.saveAndFlush(new Produto("0003 Cardapio", "Terceiro.", new BigDecimal("3.00"), true));
    }

    private String cardapio(long unidadeId, int limite) throws Exception {
        return cardapio(unidadeId, limite, token());
    }

    private String cardapio(long unidadeId, int limite, String token) throws Exception {
        MvcResult resultado = mockMvc.perform(get("/unidades/{id}/cardapio", unidadeId)
                        .header("Authorization", "Bearer " + token)
                        .param("limit", Integer.toString(limite)))
                .andExpect(status().isOk())
                .andReturn();
        return resultado.getResponse().getContentAsString();
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
        return unidades.findAll().stream()
                .filter(unidade -> unidade.getNome().equals(nome))
                .findFirst()
                .orElseThrow();
    }

    private long produtosAtivos() {
        return produtos.findAll().stream().filter(Produto::isAtivo).count();
    }

    private int paginas(long total, int limite) {
        return total == 0 ? 0 : (int) Math.ceil(total / (double) limite);
    }

    private List<String> nomes(String corpo) {
        return JsonPath.read(corpo, "$.itens[*].nome");
    }

    private int quantidade(String corpo, String nome) {
        List<Integer> valores = JsonPath.read(corpo, "$.itens[?(@.nome == '" + nome + "')].quantidadeDisponivel");
        assertEquals(1, valores.size());
        return valores.getFirst();
    }

    private boolean disponivel(String corpo, String nome) {
        List<Boolean> valores = JsonPath.read(corpo, "$.itens[?(@.nome == '" + nome + "')].disponivel");
        assertEquals(1, valores.size());
        return valores.getFirst();
    }

    private double preco(String corpo, String nome) {
        List<Number> valores = JsonPath.read(corpo, "$.itens[?(@.nome == '" + nome + "')].preco");
        assertEquals(1, valores.size());
        return valores.getFirst().doubleValue();
    }

    private long produtoId(String corpo, String nome) {
        List<Number> valores = JsonPath.read(corpo, "$.itens[?(@.nome == '" + nome + "')].produtoId");
        assertEquals(1, valores.size());
        return valores.getFirst().longValue();
    }
}
