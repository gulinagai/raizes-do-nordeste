package br.edu.uninter.raizes.infrastructure.pagamento;


import br.edu.uninter.raizes.domain.enumeracao.ResultadoSimulado;
import br.edu.uninter.raizes.domain.enumeracao.StatusPagamento;
import org.springframework.stereotype.Component;

@Component
public class SimuladorDePagamento {

    public RetornoSimulado simular(ResultadoSimulado resultado) {
        if (resultado == ResultadoSimulado.INDISPONIVEL) {
            return RetornoSimulado.indisponivel();
        }
        return RetornoSimulado.processado(StatusPagamento.valueOf(resultado.name()));
    }
}
