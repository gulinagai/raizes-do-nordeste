package br.edu.uninter.raizes.infrastructure.persistencia;

import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.uninter.raizes.domain.entidade.Fidelidade;
import br.edu.uninter.raizes.domain.entidade.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FidelidadePersistenciaTest {

    @Autowired
    private UsuarioRepositorio usuarios;

    @Autowired
    private FidelidadeRepositorio fidelidades;

    @Test
    void rejeitaSaldoNegativo() {
        Usuario cliente = usuarios.findByEmail("cliente@raizes.test").orElseThrow();
        Fidelidade fidelidade = fidelidades.saveAndFlush(new Fidelidade(cliente, true));
        fidelidade.setSaldo(-1);

        assertThrows(DataIntegrityViolationException.class, () -> fidelidades.saveAndFlush(fidelidade));
    }
}
