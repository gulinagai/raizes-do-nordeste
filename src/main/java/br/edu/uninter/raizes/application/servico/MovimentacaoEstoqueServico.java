package br.edu.uninter.raizes.application.servico;

import br.edu.uninter.raizes.domain.entidade.Estoque;
import br.edu.uninter.raizes.domain.entidade.MovimentacaoEstoque;
import br.edu.uninter.raizes.domain.entidade.Produto;
import br.edu.uninter.raizes.domain.entidade.Unidade;
import br.edu.uninter.raizes.domain.enumeracao.TipoMovimentacaoEstoque;
import br.edu.uninter.raizes.domain.excecao.EstoqueInsuficienteException;
import br.edu.uninter.raizes.domain.excecao.RecursoNaoEncontradoException;
import br.edu.uninter.raizes.infrastructure.persistencia.EstoqueRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.MovimentacaoEstoqueRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.ProdutoRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UnidadeRepositorio;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovimentacaoEstoqueServico {

    private final UnidadeRepositorio unidades;
    private final ProdutoRepositorio produtos;
    private final EstoqueRepositorio estoques;
    private final MovimentacaoEstoqueRepositorio movimentacoes;

    public MovimentacaoEstoqueServico(
            UnidadeRepositorio unidades,
            ProdutoRepositorio produtos,
            EstoqueRepositorio estoques,
            MovimentacaoEstoqueRepositorio movimentacoes) {
        this.unidades = unidades;
        this.produtos = produtos;
        this.estoques = estoques;
        this.movimentacoes = movimentacoes;
    }

    @Transactional
    public ResultadoMovimentacao registrar(
            Long unidadeId, Long produtoId, TipoMovimentacaoEstoque tipo, int quantidade, String observacao) {
        Unidade unidade = unidades.findById(unidadeId).orElseThrow(RecursoNaoEncontradoException::new);
        Produto produto = produtos.findById(produtoId).orElseThrow(RecursoNaoEncontradoException::new);
        Optional<Estoque> existente = estoques.findByUnidadeIdAndProdutoId(unidadeId, produtoId);
        Estoque estoque = tipo == TipoMovimentacaoEstoque.ENTRADA
                ? entrar(unidade, produto, existente, quantidade)
                : sair(existente, quantidade);

        try {
            Estoque salvo = estoques.saveAndFlush(estoque);
            MovimentacaoEstoque movimentacao = movimentacoes.saveAndFlush(
                    new MovimentacaoEstoque(salvo, tipo, quantidade, normalizar(observacao)));
            return new ResultadoMovimentacao(
                    movimentacao.getId(),
                    salvo.getUnidade().getId(),
                    salvo.getProduto().getId(),
                    movimentacao.getTipo(),
                    movimentacao.getQuantidade(),
                    salvo.getQuantidadeTotal(),
                    salvo.getQuantidadeReservada(),
                    salvo.quantidadeDisponivel(),
                    movimentacao.getDataHora());
        } catch (DataIntegrityViolationException excecao) {
            throw new EstoqueInsuficienteException(estoque.quantidadeDisponivel());
        }
    }

    private Estoque entrar(Unidade unidade, Produto produto, Optional<Estoque> existente, int quantidade) {
        if (existente.isEmpty()) {
            return new Estoque(unidade, produto, quantidade, 0);
        }
        Estoque estoque = existente.get();
        estoque.setQuantidadeTotal(estoque.getQuantidadeTotal() + quantidade);
        return estoque;
    }

    private Estoque sair(Optional<Estoque> existente, int quantidade) {
        Estoque estoque = existente.orElseThrow(() -> new EstoqueInsuficienteException(0));
        if (quantidade > estoque.quantidadeDisponivel()) {
            throw new EstoqueInsuficienteException(estoque.quantidadeDisponivel());
        }
        estoque.setQuantidadeTotal(estoque.getQuantidadeTotal() - quantidade);
        return estoque;
    }

    private String normalizar(String observacao) {
        if (observacao == null) {
            return null;
        }
        String texto = observacao.trim();
        return texto.isEmpty() ? null : texto;
    }
}
