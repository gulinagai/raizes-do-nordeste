package br.edu.uninter.raizes.infrastructure.persistencia;

import br.edu.uninter.raizes.domain.entidade.Fidelidade;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FidelidadeRepositorio extends JpaRepository<Fidelidade, Long> {

    Optional<Fidelidade> findByUsuarioId(Long usuarioId);
}
