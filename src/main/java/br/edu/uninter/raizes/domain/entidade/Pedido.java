package br.edu.uninter.raizes.domain.entidade;

import br.edu.uninter.raizes.domain.enumeracao.CanalPedido;
import br.edu.uninter.raizes.domain.enumeracao.StatusPedido;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_pedido", nullable = false, length = 20)
    private CanalPedido canalPedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPedido status;

    @Column(name = "data_criacao", nullable = false)
    private Instant dataCriacao;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<ItemPedido> itens = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(Unidade unidade, Usuario cliente, CanalPedido canalPedido, Instant dataCriacao) {
        this.unidade = unidade;
        this.cliente = cliente;
        this.canalPedido = canalPedido;
        this.status = StatusPedido.AGUARDANDO_PAGAMENTO;
        this.dataCriacao = dataCriacao;
        this.valorTotal = BigDecimal.ZERO;
    }

    public void adicionarItem(ItemPedido item) {
        itens.add(item);
    }

    public void definirValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public Long getId() {
        return id;
    }

    public Unidade getUnidade() {
        return unidade;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public CanalPedido getCanalPedido() {
        return canalPedido;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }
}
