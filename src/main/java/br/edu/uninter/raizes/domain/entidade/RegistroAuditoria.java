package br.edu.uninter.raizes.domain.entidade;

import br.edu.uninter.raizes.domain.enumeracao.AcaoAuditoria;
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
@Table(name = "registro_auditoria")
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AcaoAuditoria acao;

    @Column(nullable = false, length = 30)
    private String recurso;

    @Column(name = "recurso_id")
    private Long recursoId;

    @Column(length = 30)
    private String resultado;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora;

    protected RegistroAuditoria() {
    }

    public RegistroAuditoria(
            Usuario usuario, AcaoAuditoria acao, String recurso, Long recursoId, String resultado) {
        this.usuario = usuario;
        this.acao = acao;
        this.recurso = recurso;
        this.recursoId = recursoId;
        this.resultado = resultado;
        this.dataHora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public AcaoAuditoria getAcao() {
        return acao;
    }

    public String getRecurso() {
        return recurso;
    }

    public Long getRecursoId() {
        return recursoId;
    }

    public String getResultado() {
        return resultado;
    }

    public Instant getDataHora() {
        return dataHora;
    }
}
