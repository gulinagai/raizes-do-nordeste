package br.edu.uninter.raizes.api.dto;

import br.edu.uninter.raizes.domain.enumeracao.TipoMovimentacaoEstoque;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Entrada ou saída manual de estoque de um produto em uma unidade.")
public record MovimentacaoEstoqueRequisicao(
        @NotNull(message = "é obrigatório")
        @Schema(description = "Identificador da unidade.", example = "1")
        Long unidadeId,
        @NotNull(message = "é obrigatório")
        @Schema(description = "Identificador do produto.", example = "1")
        Long produtoId,
        @NotNull(message = "é obrigatório")
        @Schema(description = "ENTRADA soma ao estoque físico. SAIDA só retira o que está disponível.", example = "ENTRADA")
        TipoMovimentacaoEstoque tipo,
        @NotNull(message = "é obrigatória")
        @Min(value = 1, message = "deve ser maior que zero")
        @Schema(description = "Quantidade movimentada.", example = "2")
        Integer quantidade,
        @Size(max = 500, message = "deve ter no máximo 500 caracteres")
        @Schema(description = "Observação opcional. Não é copiada para a auditoria.", example = "Reposição da manhã")
        String observacao) {
}
