package br.edu.uninter.raizes.api.erro;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public record ErroResposta(
        String error,
        String message,
        List<DetalheErro> details,
        String timestamp,
        String path,
        String requestId) {

    public static ErroResposta de(String error, String message, List<DetalheErro> details, String path) {
        return new ErroResposta(
                error,
                message,
                details,
                DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)),
                path,
                UUID.randomUUID().toString());
    }
}
