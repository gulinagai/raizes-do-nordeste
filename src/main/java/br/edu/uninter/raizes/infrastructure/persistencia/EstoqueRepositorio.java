package br.edu.uninter.raizes.infrastructure.persistencia;

import br.edu.uninter.raizes.domain.entidade.Estoque;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstoqueRepositorio extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findByUnidadeIdAndProdutoId(Long unidadeId, Long produtoId);

    List<Estoque> findByUnidadeId(Long unidadeId);
}
