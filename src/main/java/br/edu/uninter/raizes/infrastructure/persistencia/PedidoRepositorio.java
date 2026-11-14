package br.edu.uninter.raizes.infrastructure.persistencia;

import br.edu.uninter.raizes.domain.entidade.Pedido;
import br.edu.uninter.raizes.domain.enumeracao.CanalPedido;
import br.edu.uninter.raizes.domain.enumeracao.StatusPedido;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepositorio extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {

    @Query("""
            select distinct p from Pedido p
            left join fetch p.unidade
            left join fetch p.cliente
            left join fetch p.itens item
            left join fetch item.produto
            where p.id in :ids
            """)
    List<Pedido> buscarComItens(@Param("ids") List<Long> ids);

    default Page<Pedido> listar(Long clienteId, CanalPedido canal, StatusPedido status, Pageable pagina) {
        return findAll((raiz, consulta, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (clienteId != null) {
                predicados.add(cb.equal(raiz.get("cliente").get("id"), clienteId));
            }
            if (canal != null) {
                predicados.add(cb.equal(raiz.get("canalPedido"), canal));
            }
            if (status != null) {
                predicados.add(cb.equal(raiz.get("status"), status));
            }
            if (predicados.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        }, pagina);
    }
}
