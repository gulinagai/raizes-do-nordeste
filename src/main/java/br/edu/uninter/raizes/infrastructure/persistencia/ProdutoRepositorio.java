package br.edu.uninter.raizes.infrastructure.persistencia;

import br.edu.uninter.raizes.domain.entidade.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepositorio extends JpaRepository<Produto, Long> {
}
