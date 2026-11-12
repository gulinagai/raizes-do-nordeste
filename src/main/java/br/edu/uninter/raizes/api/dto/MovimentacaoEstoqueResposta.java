package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.ResultadoMovimentacao;
import br.edu.uninter.raizes.domain.enumeracao.TipoMovimentacaoEstoque;
import java.time.Instant;

public record MovimentacaoEstoqueResposta(
        Long id,
        Long unidadeId,
        Long produtoId,
        TipoMovimentacaoEstoque tipo,
        int quantidade,
        int quantidadeTotal,
        int quantidadeReservada,
        int quantidadeDisponivel,
        Instant dataHora) {

    public static MovimentacaoEstoqueResposta de(ResultadoMovimentacao resultado) {
        return new MovimentacaoEstoqueResposta(
                resultado.id(),
                resultado.unidadeId(),
                resultado.produtoId(),
                resultado.tipo(),
                resultado.quantidade(),
                resultado.quantidadeTotal(),
                resultado.quantidadeReservada(),
                resultado.quantidadeDisponivel(),
                resultado.dataHora());
    }
}
