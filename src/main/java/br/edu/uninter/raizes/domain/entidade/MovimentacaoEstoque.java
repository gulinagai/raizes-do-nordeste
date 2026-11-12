package br.edu.uninter.raizes.domain.entidade;

import br.edu.uninter.raizes.domain.enumeracao.TipoMovimentacaoEstoque;
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
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "movimentacao_estoque")
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "estoque_id", nullable = false)
    private Estoque estoque;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentacaoEstoque tipo;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora;

    @Column(length = 500)
    private String observacao;

    protected MovimentacaoEstoque() {
    }

    public MovimentacaoEstoque(Estoque estoque, TipoMovimentacaoEstoque tipo, int quantidade, String observacao) {
        this.estoque = estoque;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.observacao = observacao;
        this.dataHora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public Long getId() {
        return id;
    }

    public Estoque getEstoque() {
        return estoque;
    }

    public TipoMovimentacaoEstoque getTipo() {
        return tipo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public Instant getDataHora() {
        return dataHora;
    }

    public String getObservacao() {
        return observacao;
    }
}
