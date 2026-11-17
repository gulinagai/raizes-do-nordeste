package br.edu.uninter.raizes.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uninter.raizes.domain.entidade.Estoque;
import br.edu.uninter.raizes.domain.entidade.Fidelidade;
import br.edu.uninter.raizes.domain.entidade.Pagamento;
import br.edu.uninter.raizes.domain.entidade.Pedido;
import br.edu.uninter.raizes.domain.entidade.Produto;
import br.edu.uninter.raizes.domain.entidade.Unidade;
import br.edu.uninter.raizes.domain.enumeracao.StatusPedido;
import br.edu.uninter.raizes.infrastructure.persistencia.EstoqueRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.FidelidadeRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.PagamentoRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.PedidoRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.ProdutoRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UnidadeRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.UUID;
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
class PontosFidelidadeTest {

    private static final String SENHA = "Raizes@2026";
    private static final String CLIENTE = "cliente@raizes.test";
    private static final String COZINHA = "cozinha@raizes.test";
    private static final String ATENDENTE = "atendente@raizes.test";

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

    @Autowired
    private FidelidadeRepositorio fidelidades;

    @Autowired
    private PagamentoRepositorio pagamentos;

    @Test
    void entregaComConsentimentoCreditaUmaVez() throws Exception {
        consentir(CLIENTE);
        long pedidoId = criar(1);
        pagar(pedidoId, "APROVADO");
        atualizar(pedidoId, "PRONTO", COZINHA);
        assertEquals(0, saldo());

        assertEquals(200, atualizar(pedidoId, "ENTREGUE", ATENDENTE).getResponse().getStatus());

        assertEquals(34, saldo());
        assertTrue(pedidos.findById(pedidoId).orElseThrow().isPontosCreditados());
        assertEquals(409, atualizar(pedidoId, "ENTREGUE", ATENDENTE).getResponse().getStatus());
        assertEquals(34, saldo());
    }

    @Test
    void entregaSemConsentimentoNaoCredita() throws Exception {
        long pedidoId = entregar(criar(1));

        assertEquals(0, saldo());
        assertTrue(fidelidades.findByUsuarioId(clienteId()).isEmpty());
        assertTrue(pedidos.findById(pedidoId).orElseThrow().isPontosCreditados());
    }

    @Test
    void resgateAplicaDescontoAntesDoPagamento() throws Exception {
        consentir(CLIENTE);
        entregar(criar(3));
        assertEquals(104, saldo());
        long pedidoId = criar(1);

        mockMvc.perform(post("/fidelidade/resgates")
                        .header("Authorization", "Bearer " + token(CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resgate(pedidoId, 100)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pontos").value(100))
                .andExpect(jsonPath("$.saldo").value(4));

        Pedido pedido = pedidos.findById(pedidoId).orElseThrow();
        assertEquals(0, new BigDecimal("1.00").compareTo(pedido.getDesconto()));
        assertEquals(0, new BigDecimal("33.90").compareTo(pedido.getValorTotal()));
        assertEquals(4, saldo());

        pagar(pedidoId, "APROVADO");
        assertEquals(0, new BigDecimal("33.90").compareTo(pagamentoDe(pedidoId).getValor()));
        assertEquals(4, saldo());
    }

    @Test
    void saldoInsuficienteNaoAlteraPedido() throws Exception {
        consentir(CLIENTE);
        entregar(criar(1));
        long pedidoId = criar(1);

        mockMvc.perform(post("/fidelidade/resgates")
                        .header("Authorization", "Bearer " + token(CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resgate(pedidoId, 100)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("RESGATE_NAO_PERMITIDO"))
                .andExpect(jsonPath("$.message").value("Saldo de pontos insuficiente."));

        assertEquals(34, saldo());
        assertEquals(0, new BigDecimal("34.90").compareTo(pedidos.findById(pedidoId).orElseThrow().getValorTotal()));
    }

    @Test
    void pontosInvalidosRetorna422() throws Exception {
        consentir(CLIENTE);
        long pedidoId = criar(1);

        mockMvc.perform(post("/fidelidade/resgates")
                        .header("Authorization", "Bearer " + token(CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resgate(pedidoId, 0)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("VALIDACAO"))
                .andExpect(jsonPath("$.details[?(@.field == 'pontos')].issue").value("deve ser maior que zero"));

        assertEquals(0, saldo());
    }

    @Test
    void clienteNaoResgataPedidoAlheio() throws Exception {
        consentir(CLIENTE);
        long pedidoId = criar(1);
        String outro = "outro." + UUID.randomUUID() + "@raizes.test";
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Outro Cliente","email":"%s","senha":"%s"}
                                """.formatted(outro, SENHA)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/fidelidade/resgates")
                        .header("Authorization", "Bearer " + token(outro))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resgate(pedidoId, 1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACESSO_NEGADO"));

        Pedido pedido = pedidos.findById(pedidoId).orElseThrow();
        assertEquals(0, pedido.getPontosResgatados());
        assertEquals(0, new BigDecimal("34.90").compareTo(pedido.getValorTotal()));
    }

    @Test
    void descontoNaoSuperaTotal() throws Exception {
        consentir(CLIENTE);
        Fidelidade conta = fidelidades.findByUsuarioId(clienteId()).orElseThrow();
        conta.setSaldo(3500);
        fidelidades.saveAndFlush(conta);
        long pedidoId = criar(1);

        mockMvc.perform(post("/fidelidade/resgates")
                        .header("Authorization", "Bearer " + token(CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resgate(pedidoId, 3500)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("O desconto não pode superar o total do pedido."));

        assertEquals(3500, saldo());
        assertEquals(0, new BigDecimal("34.90").compareTo(pedidos.findById(pedidoId).orElseThrow().getValorTotal()));
    }

    @Test
    void pagamentoRecusadoDevolvePontos() throws Exception {
        consentir(CLIENTE);
        entregar(criar(3));
        long pedidoId = criar(1);
        resgatar(pedidoId, 100);
        assertEquals(4, saldo());

        pagar(pedidoId, "RECUSADO");

        assertEquals(104, saldo());
        Pedido pedido = pedidos.findById(pedidoId).orElseThrow();
        assertEquals(StatusPedido.PAGAMENTO_RECUSADO, pedido.getStatus());
        assertTrue(pedido.isResgateEstornado());
    }

    @Test
    void pagamentoIndisponivelMantemOResgate() throws Exception {
        consentir(CLIENTE);
        entregar(criar(3));
        long pedidoId = criar(1);
        resgatar(pedidoId, 100);

        mockMvc.perform(post("/pagamentos")
                        .header("Authorization", "Bearer " + token(CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId":%d,"resultadoSimulado":"INDISPONIVEL"}
                                """.formatted(pedidoId)))
                .andExpect(status().isServiceUnavailable());

        assertEquals(4, saldo());
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, pedidos.findById(pedidoId).orElseThrow().getStatus());
        assertFalse(pedidos.findById(pedidoId).orElseThrow().isResgateEstornado());

        pagar(pedidoId, "APROVADO");
        assertEquals(4, saldo());
        assertEquals(0, new BigDecimal("33.90").compareTo(pagamentoDe(pedidoId).getValor()));
    }

    @Test
    void cancelamentoDevolvePontos() throws Exception {
        consentir(CLIENTE);
        entregar(criar(3));
        long pedidoId = criar(1);
        resgatar(pedidoId, 100);
        pagar(pedidoId, "APROVADO");

        mockMvc.perform(patch("/pedidos/{id}/cancelamento", pedidoId)
                        .header("Authorization", "Bearer " + token(CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        assertEquals(104, saldo());
        assertTrue(pedidos.findById(pedidoId).orElseThrow().isResgateEstornado());
    }

    @Test
    void revogacaoImpedeNovasOperacoesEPreservaSaldo() throws Exception {
        consentir(CLIENTE);
        entregar(criar(1));
        assertEquals(34, saldo());

        consentir(CLIENTE, false);
        entregar(criar(1));
        assertEquals(34, saldo());

        long pedidoId = criar(1);
        mockMvc.perform(post("/fidelidade/resgates")
                        .header("Authorization", "Bearer " + token(CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resgate(pedidoId, 10)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("O cliente não participa do programa de fidelização."));
        assertEquals(34, saldo());

        consentir(CLIENTE, true);
        assertEquals(34, saldo());
        entregar(criar(1));
        assertEquals(68, saldo());
    }

    private void consentir(String email) throws Exception {
        consentir(email, true);
    }

    private void consentir(String email, boolean aceito) throws Exception {
        mockMvc.perform(put("/fidelidade")
                        .header("Authorization", "Bearer " + token(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentimento\":" + aceito + "}"))
                .andExpect(status().isOk());
    }

    private long criar(int quantidade) throws Exception {
        Estoque estoque = estoque();
        MvcResult pedido = mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + token(CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unidadeId":%d,"clienteId":%d,"canalPedido":"APP","itens":[{"produtoId":%d,"quantidade":%d}]}
                                """.formatted(estoque.getUnidade().getId(), clienteId(), estoque.getProduto().getId(), quantidade)))
                .andExpect(status().isCreated())
                .andReturn();
        return numero(pedido.getResponse().getContentAsString(), "$.id");
    }

    private void pagar(long pedidoId, String resultado) throws Exception {
        mockMvc.perform(post("/pagamentos")
                        .header("Authorization", "Bearer " + token(CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId":%d,"resultadoSimulado":"%s"}
                                """.formatted(pedidoId, resultado)))
                .andExpect(status().isCreated());
    }

    private long entregar(long pedidoId) throws Exception {
        pagar(pedidoId, "APROVADO");
        assertEquals(200, atualizar(pedidoId, "PRONTO", COZINHA).getResponse().getStatus());
        assertEquals(200, atualizar(pedidoId, "ENTREGUE", ATENDENTE).getResponse().getStatus());
        return pedidoId;
    }

    private MvcResult atualizar(long pedidoId, String statusPedido, String email) throws Exception {
        return mockMvc.perform(patch("/pedidos/{id}/status", pedidoId)
                        .header("Authorization", "Bearer " + token(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"%s"}
                                """.formatted(statusPedido)))
                .andReturn();
    }

    private void resgatar(long pedidoId, int pontos) throws Exception {
        mockMvc.perform(post("/fidelidade/resgates")
                        .header("Authorization", "Bearer " + token(CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resgate(pedidoId, pontos)))
                .andExpect(status().isCreated());
    }

    private String resgate(long pedidoId, int pontos) {
        return """
                {"pedidoId":%d,"pontos":%d}
                """.formatted(pedidoId, pontos);
    }

    private int saldo() {
        return fidelidades.findByUsuarioId(clienteId()).map(Fidelidade::getSaldo).orElse(0);
    }

    private long clienteId() {
        return usuarios.findByEmail(CLIENTE).orElseThrow().getId();
    }

    private Pagamento pagamentoDe(long pedidoId) {
        return pagamentos.findAll().stream()
                .filter(pagamento -> pagamento.getPedido().getId().equals(pedidoId))
                .findFirst()
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
