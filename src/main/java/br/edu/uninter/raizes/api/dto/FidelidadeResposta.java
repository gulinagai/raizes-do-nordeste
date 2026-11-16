package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.SituacaoFidelidade;
import java.time.Instant;

public record FidelidadeResposta(
        boolean consentimento,
        Instant dataConsentimento,
        Instant dataRevogacao,
        int saldo) {

    public static FidelidadeResposta de(SituacaoFidelidade situacao) {
        return new FidelidadeResposta(
                situacao.consentimento(),
                situacao.dataConsentimento(),
                situacao.dataRevogacao(),
                situacao.saldo());
    }
}
