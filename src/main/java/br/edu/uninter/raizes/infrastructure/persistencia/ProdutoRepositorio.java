package br.edu.uninter.raizes.infrastructure.persistencia;

import br.edu.uninter.raizes.domain.entidade.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProdutoRepositorio extends JpaRepository<Produto, Long> {

    @Query(
            value = """
                    SELECT new br.edu.uninter.raizes.infrastructure.persistencia.LinhaCardapio(
                        p.id,
                        p.nome,
                        p.descricao,
                        p.preco,
                        COALESCE(e.quantidadeTotal, 0) - COALESCE(e.quantidadeReservada, 0))
                    FROM Produto p
                    LEFT JOIN Estoque e ON e.produto = p AND e.unidade.id = :unidadeId
                    WHERE p.ativo = true
                    ORDER BY p.nome ASC, p.id ASC
                    """,
            countQuery = "SELECT COUNT(p) FROM Produto p WHERE p.ativo = true")
    Page<LinhaCardapio> listarCardapio(@Param("unidadeId") Long unidadeId, Pageable pageable);
}
