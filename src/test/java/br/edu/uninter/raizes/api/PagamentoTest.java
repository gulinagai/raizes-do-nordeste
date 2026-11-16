package br.edu.uninter.raizes.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uninter.raizes.domain.entidade.Estoque;
import br.edu.uninter.raizes.domain.entidade.MovimentacaoEstoque;
import br.edu.uninter.raizes.domain.entidade.Produto;
import br.edu.uninter.raizes.domain.entidade.Unidade;
import br.edu.uninter.raizes.domain.enumeracao.TipoMovimentacaoEstoque;
import br.edu.uninter.raizes.infrastructure.persistencia.EstoqueRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.MovimentacaoEstoqueRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.PagamentoRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.ProdutoRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UnidadeRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
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
class PagamentoTest {

    private static final String EMAIL = "cliente@raizes.test";
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
    private MovimentacaoEstoqueRepositorio movimentacoes;

    @Autowired
    private PagamentoRepositorio pagamentos;

    @Test
    void aprovaPagamentoConsomeReservaERegistraSaida() throws Exception {
        long pedidoId = prepararPedido();
        long estoqueId = estoque().getId();
        long saidasAntes = saidas(estoqueId);

        MvcResult resultado = pagar(pedidoId, "APROVADO");

        assertEquals(201, resultado.getResponse().getStatus());
        String corpo = resultado.getResponse().getContentAsString();
        assertEquals("APROVADO", JsonPath.read(corpo, "$.status"));
        assertEquals("EM_PREPARO", JsonPath.read(corpo, "$.statusPedido"));
        assertEquals(pedidoId, numero(corpo, "$.pedidoId"));
        assertEquals(0, new BigDecimal("69.80").compareTo(new BigDecimal(JsonPath.read(corpo, "$.valor").toString())));

        Estoque atualizado = estoque();
        assertEquals(18, atualizado.getQuantidadeTotal());
        assertEquals(3, atualizado.getQuantidadeReservada());
        assertEquals(15, atualizado.quantidadeDisponivel());
        assertEquals(saidasAntes + 1, saidas(estoqueId));
        assertEquals(2, ultimaSaida(estoqueId).getQuantidade());

        MvcResult repetido = pagar(pedidoId, "APROVADO");
        assertEquals(409, repetido.getResponse().getStatus());
        assertEquals("PAGAMENTO_NAO_PERMITIDO", JsonPath.read(repetido.getResponse().getContentAsString(), "$.error"));
        assertEquals(18, estoque().getQuantidadeTotal());
        assertEquals(3, estoque().getQuantidadeReservada());
        assertEquals(1, pagamentosDo(pedidoId));
    }


    @Test
    void recusaPagamentoLiberaReservaSemBaixa() throws Exception {
        long pedidoId = prepararPedido();
        long estoqueId = estoque().getId();
        long saidasAntes = saidas(estoqueId);

        MvcResult resultado = pagar(pedidoId, "RECUSADO");

        assertEquals(201, resultado.getResponse().getStatus());
        String corpo = resultado.getResponse().getContentAsString();
        assertEquals("RECUSADO", JsonPath.read(corpo, "$.status"));
        assertEquals("PAGAMENTO_RECUSADO", JsonPath.read(corpo, "$.statusPedido"));
        Estoque atualizado = estoque();
        assertEquals(20, atualizado.getQuantidadeTotal());
        assertEquals(3, atualizado.getQuantidadeReservada());
        assertEquals(17, atualizado.quantidadeDisponivel());
        assertEquals(saidasAntes, saidas(estoqueId));

        assertEquals(409, pagar(pedidoId, "APROVADO").getResponse().getStatus());
        assertEquals(20, estoque().getQuantidadeTotal());
        assertEquals(3, estoque().getQuantidadeReservada());
    }

    @Test
    void indisponivelMantemPedidoEPermiteNovaTentativa() throws Exception {
        long pedidoId = prepararPedido();

        MvcResult indisponivel = pagar(pedidoId, "INDISPONIVEL");

        assertEquals(503, indisponivel.getResponse().getStatus());
        assertEquals("GATEWAY_PAGAMENTO_INDISPONIVEL", JsonPath.read(indisponivel.getResponse().getContentAsString(), "$.error"));
        assertEquals(20, estoque().getQuantidadeTotal());
        assertEquals(5, estoque().getQuantidadeReservada());
        assertEquals(0, pagamentosDo(pedidoId));

        assertEquals(201, pagar(pedidoId, "APROVADO").getResponse().getStatus());
        assertEquals(18, estoque().getQuantidadeTotal());
        assertEquals(3, estoque().getQuantidadeReservada());
    }

    @Test
    void rejeitaPedidoInexistenteOuResultadoInvalido() throws Exception {
        mockMvc.perform(post("/pagamentos")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId":999999999,"resultadoSimulado":"APROVADO"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NAO_ENCONTRADO"));

        long pedidoId = prepararPedido();
        mockMvc.perform(post("/pagamentos")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId":%d,"resultadoSimulado":"ESTORNADO"}
                                """.formatted(pedidoId)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("VALIDACAO"));

        mockMvc.perform(post("/pagamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId":%d,"resultadoSimulado":"APROVADO"}
                                """.formatted(pedidoId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NAO_AUTENTICADO"));

        assertEquals(20, estoque().getQuantidadeTotal());
        assertEquals(5, estoque().getQuantidadeReservada());
    }

    private long prepararPedido() throws Exception {
        Estoque estoque = estoque();
        estoque.setQuantidadeTotal(20);
        estoque.setQuantidadeReservada(3);
        estoques.saveAndFlush(estoque);
        long clienteId = usuarios.findByEmail(EMAIL).orElseThrow().getId();
        MvcResult pedido = mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unidadeId":%d,"clienteId":%d,"canalPedido":"APP","itens":[{"produtoId":%d,"quantidade":2}]}
                                """.formatted(estoque.getUnidade().getId(), clienteId, estoque.getProduto().getId())))
                .andExpect(status().isCreated())
                .andReturn();
        return numero(pedido.getResponse().getContentAsString(), "$.id");
    }

    private MvcResult pagar(long pedidoId, String resultado) throws Exception {
        return mockMvc.perform(post("/pagamentos")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId":%d,"resultadoSimulado":"%s"}
                                """.formatted(pedidoId, resultado)))
                .andReturn();
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

    private Estoque estoque() {
        Unidade unidade = unidades.findAll().stream().filter(item -> item.getNome().equals(MATRIZ)).findFirst().orElseThrow();
        Produto produto = produtos.findAll().stream().filter(item -> item.getNome().equals(BAIAO)).findFirst().orElseThrow();
        return estoques.findByUnidadeIdAndProdutoId(unidade.getId(), produto.getId()).orElseThrow();
    }

    private long saidas(long estoqueId) {
        return movimentacoes.findAll().stream()
                .filter(movimento -> movimento.getTipo() == TipoMovimentacaoEstoque.SAIDA)
                .filter(movimento -> movimento.getEstoque().getId().equals(estoqueId))
                .count();
    }

    private MovimentacaoEstoque ultimaSaida(long estoqueId) {
        return movimentacoes.findAll().stream()
                .filter(movimento -> movimento.getTipo() == TipoMovimentacaoEstoque.SAIDA)
                .filter(movimento -> movimento.getEstoque().getId().equals(estoqueId))
                .reduce((primeira, segunda) -> segunda)
                .orElseThrow();
    }

    private long pagamentosDo(long pedidoId) {
        return pagamentos.findAll().stream()
                .filter(pagamento -> pagamento.getPedido().getId().equals(pedidoId))
                .count();
    }

    private long numero(String corpo, String caminho) {
        return ((Number) JsonPath.read(corpo, caminho)).longValue();
    }
}
