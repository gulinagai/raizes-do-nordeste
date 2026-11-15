package br.edu.uninter.raizes.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uninter.raizes.domain.entidade.Estoque;
import br.edu.uninter.raizes.domain.entidade.MovimentacaoEstoque;
import br.edu.uninter.raizes.domain.entidade.Produto;
import br.edu.uninter.raizes.domain.entidade.Unidade;
import br.edu.uninter.raizes.domain.enumeracao.StatusPagamento;
import br.edu.uninter.raizes.domain.enumeracao.StatusPedido;
import br.edu.uninter.raizes.domain.enumeracao.TipoMovimentacaoEstoque;
import br.edu.uninter.raizes.infrastructure.persistencia.EstoqueRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.MovimentacaoEstoqueRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.PagamentoRepositorio;
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
class CancelamentoPedidoTest {

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
    private PedidoRepositorio pedidos;

    @Autowired
    private PagamentoRepositorio pagamentos;

    @Autowired
    private MovimentacaoEstoqueRepositorio movimentacoes;

    @Test
    void cancelaAntesDoPagamentoLiberaReserva() throws Exception {
        preparar(20, 3);
        long pedidoId = criar(2);
        long estoqueId = estoque().getId();
        long entradas = movimentacoes(estoqueId, TipoMovimentacaoEstoque.ENTRADA);

        MvcResult resultado = cancelar(pedidoId, token());

        assertEquals(200, resultado.getResponse().getStatus());
        assertEquals("CANCELADO", JsonPath.read(resultado.getResponse().getContentAsString(), "$.status"));
        assertEquals(20, estoque().getQuantidadeTotal());
        assertEquals(3, estoque().getQuantidadeReservada());
        assertEquals(17, estoque().quantidadeDisponivel());
        assertEquals(entradas, movimentacoes(estoqueId, TipoMovimentacaoEstoque.ENTRADA));
        assertEquals(409, cancelar(pedidoId, token()).getResponse().getStatus());
        assertEquals(20, estoque().getQuantidadeTotal());
        assertEquals(3, estoque().getQuantidadeReservada());
    }

    @Test
    void cancelaAposPagamentoDevolveQuantidadeFisica() throws Exception {
        preparar(20, 3);
        long pedidoId = criar(2);
        pagar(pedidoId, "APROVADO");
        long estoqueId = estoque().getId();
        long entradas = movimentacoes(estoqueId, TipoMovimentacaoEstoque.ENTRADA);

        MvcResult resultado = cancelar(pedidoId, token());

        assertEquals(200, resultado.getResponse().getStatus());
        assertEquals("CANCELADO", JsonPath.read(resultado.getResponse().getContentAsString(), "$.status"));
        assertEquals(20, estoque().getQuantidadeTotal());
        assertEquals(3, estoque().getQuantidadeReservada());
        assertEquals(17, estoque().quantidadeDisponivel());
        assertEquals(entradas + 1, movimentacoes(estoqueId, TipoMovimentacaoEstoque.ENTRADA));
        assertEquals(2, ultima(estoqueId, TipoMovimentacaoEstoque.ENTRADA).getQuantidade());
        assertEquals(StatusPagamento.APROVADO, pagamentos.findAll().stream()
                .filter(pagamento -> pagamento.getPedido().getId().equals(pedidoId))
                .findFirst()
                .orElseThrow()
                .getStatus());

        pedidoPronto();
    }

    @Test
    void recusaNaoLiberaReservaDeNovo() throws Exception {
        preparar(20, 3);
        long pedidoId = criar(2);
        pagar(pedidoId, "RECUSADO");

        MvcResult resultado = cancelar(pedidoId, token());

        assertEquals(409, resultado.getResponse().getStatus());
        assertEquals("TRANSICAO_NAO_PERMITIDA", JsonPath.read(resultado.getResponse().getContentAsString(), "$.error"));
        assertEquals(StatusPedido.PAGAMENTO_RECUSADO, pedidos.findById(pedidoId).orElseThrow().getStatus());
        assertEquals(20, estoque().getQuantidadeTotal());
        assertEquals(3, estoque().getQuantidadeReservada());
    }

    @Test
    void rejeitaInexistenteEntregueOuSemAutenticacao() throws Exception {
        preparar(20, 3);
        long pedidoId = criar(1);
        pagar(pedidoId, "APROVADO");
        assertEquals(200, atualizar(pedidoId, "PRONTO").getResponse().getStatus());
        assertEquals(200, atualizar(pedidoId, "ENTREGUE").getResponse().getStatus());
        int total = estoque().getQuantidadeTotal();
        int reservada = estoque().getQuantidadeReservada();

        assertEquals(409, cancelar(pedidoId, token()).getResponse().getStatus());
        assertEquals(StatusPedido.ENTREGUE, pedidos.findById(pedidoId).orElseThrow().getStatus());
        assertEquals(total, estoque().getQuantidadeTotal());
        assertEquals(reservada, estoque().getQuantidadeReservada());

        mockMvc.perform(patch("/pedidos/{id}/cancelamento", 999999999L)
                        .header("Authorization", "Bearer " + token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NAO_ENCONTRADO"));

        mockMvc.perform(patch("/pedidos/{id}/cancelamento", pedidoId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NAO_AUTENTICADO"));
    }

    private void pedidoPronto() throws Exception {
        long pedidoId = criar(1);
        pagar(pedidoId, "APROVADO");
        assertEquals(200, atualizar(pedidoId, "PRONTO").getResponse().getStatus());
        int total = estoque().getQuantidadeTotal();
        int reservada = estoque().getQuantidadeReservada();
        assertEquals(200, cancelar(pedidoId, token()).getResponse().getStatus());
        assertEquals(total + 1, estoque().getQuantidadeTotal());
        assertEquals(reservada, estoque().getQuantidadeReservada());
        assertEquals(StatusPedido.CANCELADO, pedidos.findById(pedidoId).orElseThrow().getStatus());
    }

    private void preparar(int total, int reservada) {
        Estoque estoque = estoque();
        estoque.setQuantidadeTotal(total);
        estoque.setQuantidadeReservada(reservada);
        estoques.saveAndFlush(estoque);
    }

    private long criar(int quantidade) throws Exception {
        Estoque estoque = estoque();
        long clienteId = usuarios.findByEmail(EMAIL).orElseThrow().getId();
        MvcResult pedido = mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unidadeId":%d,"clienteId":%d,"canalPedido":"APP","itens":[{"produtoId":%d,"quantidade":%d}]}
                                """.formatted(estoque.getUnidade().getId(), clienteId, estoque.getProduto().getId(), quantidade)))
                .andExpect(status().isCreated())
                .andReturn();
        return numero(pedido.getResponse().getContentAsString(), "$.id");
    }

    private void pagar(long pedidoId, String resultado) throws Exception {
        mockMvc.perform(post("/pagamentos")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId":%d,"resultadoSimulado":"%s"}
                                """.formatted(pedidoId, resultado)))
                .andExpect(status().isCreated());
    }

    private MvcResult atualizar(long pedidoId, String statusPedido) throws Exception {
        String email = "PRONTO".equals(statusPedido) ? "cozinha@raizes.test" : "atendente@raizes.test";
        return mockMvc.perform(patch("/pedidos/{id}/status", pedidoId)
                        .header("Authorization", "Bearer " + token(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"%s"}
                                """.formatted(statusPedido)))
                .andReturn();
    }

    private MvcResult cancelar(long pedidoId, String token) throws Exception {
        return mockMvc.perform(patch("/pedidos/{id}/cancelamento", pedidoId)
                        .header("Authorization", "Bearer " + token))
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

    private long movimentacoes(long estoqueId, TipoMovimentacaoEstoque tipo) {
        return movimentacoes.findAll().stream()
                .filter(movimento -> movimento.getTipo() == tipo)
                .filter(movimento -> movimento.getEstoque().getId().equals(estoqueId))
                .count();
    }

    private MovimentacaoEstoque ultima(long estoqueId, TipoMovimentacaoEstoque tipo) {
        return movimentacoes.findAll().stream()
                .filter(movimento -> movimento.getTipo() == tipo)
                .filter(movimento -> movimento.getEstoque().getId().equals(estoqueId))
                .reduce((primeira, segunda) -> segunda)
                .orElseThrow();
    }

    private long numero(String corpo, String caminho) {
        return ((Number) JsonPath.read(corpo, caminho)).longValue();
    }
}
