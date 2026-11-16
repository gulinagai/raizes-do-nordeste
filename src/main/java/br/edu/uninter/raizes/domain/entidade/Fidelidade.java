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
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "fidelidade")
public class Fidelidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false)
    private boolean consentimento;

    @Column(name = "data_consentimento")
    private Instant dataConsentimento;

    @Column(name = "data_revogacao")
    private Instant dataRevogacao;

    @Column(nullable = false)
    private int saldo;

    protected Fidelidade() {
    }

    public Fidelidade(Usuario usuario, boolean consentimento) {
        this.usuario = usuario;
        this.consentimento = consentimento;
        this.saldo = 0;
        if (consentimento) {
            this.dataConsentimento = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        }
    }

    public boolean definirConsentimento(boolean aceito) {
        if (this.consentimento == aceito) {
            return false;
        }
        Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        this.consentimento = aceito;
        if (aceito) {
            this.dataConsentimento = agora;
            this.dataRevogacao = null;
        } else {
            this.dataRevogacao = agora;
        }
        return true;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public boolean isConsentimento() {
        return consentimento;
    }

    public Instant getDataConsentimento() {
        return dataConsentimento;
    }

    public Instant getDataRevogacao() {
        return dataRevogacao;
    }

    public int getSaldo() {
        return saldo;
    }

    public void setSaldo(int saldo) {
        this.saldo = saldo;
    }
}
