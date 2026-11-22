package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.application.servico.ResultadoMovimentacao;
import br.edu.uninter.raizes.domain.enumeracao.TipoMovimentacaoEstoque;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Estoque depois da movimentação registrada.")
public record MovimentacaoEstoqueResposta(
        @Schema(description = "Identificador da movimentação.", example = "10")
        Long id,
        @Schema(description = "Identificador da unidade.", example = "1")
        Long unidadeId,
        @Schema(description = "Identificador do produto.", example = "1")
        Long produtoId,
        @Schema(description = "Tipo registrado.", example = "ENTRADA")
        TipoMovimentacaoEstoque tipo,
        @Schema(description = "Quantidade da movimentação.", example = "2")
        int quantidade,
        @Schema(description = "Quantidade física depois da movimentação.", example = "42")
        int quantidadeTotal,
        @Schema(description = "Quantidade reservada por pedidos. A movimentação manual não a altera.", example = "0")
        int quantidadeReservada,
        @Schema(description = "Quantidade física menos a reservada.", example = "42")
        int quantidadeDisponivel,
        @Schema(description = "Momento da movimentação.", example = "2026-11-16T18:00:00Z")
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
