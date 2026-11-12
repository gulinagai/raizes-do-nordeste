package br.edu.uninter.raizes.infrastructure.persistencia;

import br.edu.uninter.raizes.domain.entidade.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadeRepositorio extends JpaRepository<Unidade, Long> {
}
