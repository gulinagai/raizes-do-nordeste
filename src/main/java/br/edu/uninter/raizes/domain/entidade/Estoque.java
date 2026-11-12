package br.edu.uninter.raizes.domain.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "estoque",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_estoque_unidade_produto",
                columnNames = {"unidade_id", "produto_id"}))
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(name = "quantidade_total", nullable = false)
    private int quantidadeTotal;

    @Column(name = "quantidade_reservada", nullable = false)
    private int quantidadeReservada;

    protected Estoque() {
    }

    public Estoque(Unidade unidade, Produto produto, int quantidadeTotal, int quantidadeReservada) {
        this.unidade = unidade;
        this.produto = produto;
        this.quantidadeTotal = quantidadeTotal;
        this.quantidadeReservada = quantidadeReservada;
    }

    public int quantidadeDisponivel() {
        return quantidadeTotal - quantidadeReservada;
    }

    public Long getId() {
        return id;
    }

    public Unidade getUnidade() {
        return unidade;
    }

    public void setUnidade(Unidade unidade) {
        this.unidade = unidade;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public int getQuantidadeTotal() {
        return quantidadeTotal;
    }

    public void setQuantidadeTotal(int quantidadeTotal) {
        this.quantidadeTotal = quantidadeTotal;
    }

    public int getQuantidadeReservada() {
        return quantidadeReservada;
    }

    public void setQuantidadeReservada(int quantidadeReservada) {
        this.quantidadeReservada = quantidadeReservada;
    }
}
