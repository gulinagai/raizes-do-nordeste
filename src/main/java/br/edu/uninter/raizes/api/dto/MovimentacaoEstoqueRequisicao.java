package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.domain.enumeracao.TipoMovimentacaoEstoque;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MovimentacaoEstoqueRequisicao(
        @NotNull(message = "é obrigatório") Long unidadeId,
        @NotNull(message = "é obrigatório") Long produtoId,
        @NotNull(message = "é obrigatório") TipoMovimentacaoEstoque tipo,
        @NotNull(message = "é obrigatória") @Min(value = 1, message = "deve ser maior que zero") Integer quantidade,
        @Size(max = 500, message = "deve ter no máximo 500 caracteres") String observacao) {
}
