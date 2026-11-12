package br.edu.uninter.raizes.infrastructure.persistencia;

import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.uninter.raizes.domain.entidade.Estoque;
import br.edu.uninter.raizes.domain.entidade.MovimentacaoEstoque;
import br.edu.uninter.raizes.domain.entidade.Produto;
import br.edu.uninter.raizes.domain.entidade.Unidade;
import br.edu.uninter.raizes.domain.enumeracao.TipoMovimentacaoEstoque;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EstoquePersistenciaTest {

    @Autowired
    private UnidadeRepositorio unidades;

    @Autowired
    private ProdutoRepositorio produtos;

    @Autowired
    private EstoqueRepositorio estoques;

    @Autowired
    private MovimentacaoEstoqueRepositorio movimentacoes;

    @Test
    void rejeitaSegundoEstoqueParaOMesmoPar() {
        Unidade matriz = matriz();
        Produto produto = produto();
        estoques.saveAndFlush(new Estoque(matriz, produto, 4, 1));

        assertThrows(DataIntegrityViolationException.class,
                () -> estoques.saveAndFlush(new Estoque(matriz, produto, 2, 0)));
    }

    @Test
    void rejeitaQuantidadeTotalNegativa() {
        Estoque estoque = estoques.saveAndFlush(new Estoque(matriz(), produto(), 4, 1));
        estoque.setQuantidadeTotal(-1);

        assertThrows(DataIntegrityViolationException.class, () -> estoques.saveAndFlush(estoque));
    }

    @Test
    void rejeitaQuantidadeReservadaNegativa() {
        Estoque estoque = estoques.saveAndFlush(new Estoque(matriz(), produto(), 4, 1));
        estoque.setQuantidadeReservada(-1);

        assertThrows(DataIntegrityViolationException.class, () -> estoques.saveAndFlush(estoque));
    }

    @Test
    void rejeitaReservaSuperiorAoTotal() {
        Estoque estoque = estoques.saveAndFlush(new Estoque(matriz(), produto(), 4, 1));
        estoque.setQuantidadeReservada(5);

        assertThrows(DataIntegrityViolationException.class, () -> estoques.saveAndFlush(estoque));
    }

    @Test
    void rejeitaMovimentacaoComQuantidadeNaoPositiva() {
        Estoque estoque = estoques.saveAndFlush(new Estoque(matriz(), produto(), 4, 1));

        assertThrows(DataIntegrityViolationException.class, () -> movimentacoes.saveAndFlush(
                new MovimentacaoEstoque(estoque, TipoMovimentacaoEstoque.ENTRADA, 0, null)));
    }

    private Unidade matriz() {
        return unidades.findAll().stream()
                .filter(unidade -> unidade.getNome().equals("Raízes Matriz Recife"))
                .findFirst()
                .orElseThrow();
    }

    private Produto produto() {
        return produtos.saveAndFlush(new Produto(
                "Persistencia " + UUID.randomUUID(),
                "Restrição de estoque.",
                new BigDecimal("1.00"),
                true));
    }
}
