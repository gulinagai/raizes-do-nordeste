package br.edu.uninter.raizes.domain.entidade;

import br.edu.uninter.raizes.domain.enumeracao.TipoUnidade;
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

@Entity
@Table(name = "unidade")
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 80)
    private String cidade;

    @Column(nullable = false, length = 180)
    private String endereco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoUnidade tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_matriz_id")
    private Unidade unidadeMatriz;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Unidade() {
    }

    public Unidade(String nome, String cidade, String endereco, TipoUnidade tipo, Unidade unidadeMatriz, boolean ativo) {
        this.nome = nome;
        this.cidade = cidade;
        this.endereco = endereco;
        this.tipo = tipo;
        this.unidadeMatriz = unidadeMatriz;
        this.ativo = ativo;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public TipoUnidade getTipo() {
        return tipo;
    }

    public void setTipo(TipoUnidade tipo) {
        this.tipo = tipo;
    }

    public Unidade getUnidadeMatriz() {
        return unidadeMatriz;
    }

    public void setUnidadeMatriz(Unidade unidadeMatriz) {
        this.unidadeMatriz = unidadeMatriz;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
