package br.edu.uninter.raizes.infrastructure.persistencia;

import br.edu.uninter.raizes.domain.entidade.MovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoEstoqueRepositorio extends JpaRepository<MovimentacaoEstoque, Long> {
}
