package br.edu.uninter.raizes.domain.excecao;

public class ReservaExcedidaException extends RuntimeException {

    private final int indice;
    private final int disponivel;

    public ReservaExcedidaException(int indice, int disponivel) {
        this.indice = indice;
        this.disponivel = disponivel;
    }

    public int getIndice() {
        return indice;
    }

    public int getDisponivel() {
        return disponivel;
    }
}
