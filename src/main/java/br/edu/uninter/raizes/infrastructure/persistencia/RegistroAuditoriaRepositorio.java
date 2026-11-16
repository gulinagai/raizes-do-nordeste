package br.edu.uninter.raizes.infrastructure.persistencia;

import br.edu.uninter.raizes.domain.entidade.RegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroAuditoriaRepositorio extends JpaRepository<RegistroAuditoria, Long> {
}
