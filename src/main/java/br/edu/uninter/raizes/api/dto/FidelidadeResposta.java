package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.SituacaoFidelidade;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Situação do programa de pontos do cliente autenticado.")
public record FidelidadeResposta(
        @Schema(description = "Verdadeiro quando o consentimento está ativo.", example = "true")
        boolean consentimento,
        @Schema(description = "Momento do aceite vigente. Nulo quando o cliente nunca aceitou.", example = "2026-11-16T18:10:00Z")
        Instant dataConsentimento,
        @Schema(description = "Momento da revogação. Nulo quando o consentimento está ativo ou nunca foi aceito.")
        Instant dataRevogacao,
        @Schema(description = "Saldo de pontos. Nasce em zero e não fica negativo.", example = "34")
        int saldo) {

    public static FidelidadeResposta de(SituacaoFidelidade situacao) {
        return new FidelidadeResposta(
                situacao.consentimento(),
                situacao.dataConsentimento(),
                situacao.dataRevogacao(),
                situacao.saldo());
    }
}
