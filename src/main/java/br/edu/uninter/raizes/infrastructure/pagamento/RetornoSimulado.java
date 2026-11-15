package br.edu.uninter.raizes.infrastructure.pagamento;

import br.edu.uninter.raizes.domain.enumeracao.StatusPagamento;

public record RetornoSimulado(boolean gatewayIndisponivel, StatusPagamento status) {

    public static RetornoSimulado indisponivel() {
        return new RetornoSimulado(true, null);
    }

    public static RetornoSimulado processado(StatusPagamento status) {
        return new RetornoSimulado(false, status);
    }
}
