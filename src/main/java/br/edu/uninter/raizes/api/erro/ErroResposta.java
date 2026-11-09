package br.edu.uninter.raizes.api.erro;

import java.util.List;

public record ErroResposta(
        String error,
        String message,
        List<DetalheErro> details,
        String timestamp,
        String path,
        String requestId) {
}
