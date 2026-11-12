package br.edu.uninter.raizes.application.servico;

import br.edu.uninter.raizes.domain.entidade.Unidade;
import br.edu.uninter.raizes.domain.excecao.ParametrosInvalidosException;
import br.edu.uninter.raizes.domain.excecao.ParametrosInvalidosException.Campo;
import br.edu.uninter.raizes.domain.excecao.RecursoNaoEncontradoException;
import br.edu.uninter.raizes.infrastructure.persistencia.LinhaCardapio;
import br.edu.uninter.raizes.infrastructure.persistencia.ProdutoRepositorio;
import br.edu.uninter.raizes.infrastructure.persistencia.UnidadeRepositorio;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardapioServico {

    static final int LIMITE_MAXIMO = 50;

    private final UnidadeRepositorio unidades;
    private final ProdutoRepositorio produtos;

    public CardapioServico(UnidadeRepositorio unidades, ProdutoRepositorio produtos) {
        this.unidades = unidades;
        this.produtos = produtos;
    }

    @Transactional(readOnly = true)
    public ResultadoCardapio consultar(Long unidadeId, int pagina, int limite) {
        validarPaginacao(pagina, limite);
        Unidade unidade = unidades.findById(unidadeId).orElseThrow(RecursoNaoEncontradoException::new);
        Page<LinhaCardapio> consulta = produtos.listarCardapio(unidadeId, PageRequest.of(pagina - 1, limite));
        List<ItemDeCardapio> itens = consulta.getContent().stream().map(this::paraItem).toList();
        return new ResultadoCardapio(
                unidade.getId(),
                unidade.getNome(),
                itens,
                pagina,
                limite,
                consulta.getTotalElements(),
                consulta.getTotalPages());
    }

    private ItemDeCardapio paraItem(LinhaCardapio linha) {
        int quantidadeDisponivel = linha.quantidadeDisponivel() == null ? 0 : linha.quantidadeDisponivel();
        return new ItemDeCardapio(
                linha.produtoId(),
                linha.nome(),
                linha.descricao(),
                linha.preco(),
                quantidadeDisponivel,
                quantidadeDisponivel > 0);
    }

    private void validarPaginacao(int pagina, int limite) {
        List<Campo> campos = new ArrayList<>();
        if (pagina < 1) {
            campos.add(new Campo("page", "deve ser maior ou igual a 1"));
        }
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            campos.add(new Campo("limit", "deve estar entre 1 e 50"));
        }
        if (!campos.isEmpty()) {
            throw new ParametrosInvalidosException(campos);
        }
    }
}
