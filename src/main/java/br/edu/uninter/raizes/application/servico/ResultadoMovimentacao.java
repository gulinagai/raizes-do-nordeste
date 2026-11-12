package br.edu.uninter.raizes.application.servico;

import br.edu.uninter.raizes.domain.enumeracao.TipoMovimentacaoEstoque;
import java.time.Instant;

public record ResultadoMovimentacao(
        Long id,
        Long unidadeId,
        Long produtoId,
        TipoMovimentacaoEstoque tipo,
        int quantidade,
        int quantidadeTotal,
        int quantidadeReservada,
        int quantidadeDisponivel,
        Instant dataHora) {
}
