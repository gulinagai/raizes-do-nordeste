package br.edu.uninter.raizes.application.servico;

import br.edu.uninter.raizes.domain.entidade.Fidelidade;
import java.time.Instant;

public record SituacaoFidelidade(
        boolean consentimento,
        Instant dataConsentimento,
        Instant dataRevogacao,
        int saldo) {

    public static SituacaoFidelidade ausente() {
        return new SituacaoFidelidade(false, null, null, 0);
    }

    public static SituacaoFidelidade de(Fidelidade fidelidade) {
        return new SituacaoFidelidade(
                fidelidade.isConsentimento(),
                fidelidade.getDataConsentimento(),
                fidelidade.getDataRevogacao(),
                fidelidade.getSaldo());
    }
}
