package br.edu.uninter.raizes.api.erro;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Schema(description = "Corpo padrão de todas as falhas da API.")
public record ErroResposta(
        @Schema(description = "Código estável da falha.", example = "VALIDACAO")
        String error,
        @Schema(description = "Mensagem legível.", example = "Os dados informados são inválidos.")
        String message,
        @Schema(description = "Campos inválidos. A lista fica vazia quando a falha não é de um campo.")
        List<DetalheErro> details,
        @Schema(description = "Momento da falha.", example = "2026-11-16T18:00:00Z")
        String timestamp,
        @Schema(description = "Caminho da requisição.", example = "/pedidos")
        String path,
        @Schema(description = "Identificador da ocorrência.", example = "6f1c0b5e-1d4a-4c2e-9a1b-0e5d7c8a1234")
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
