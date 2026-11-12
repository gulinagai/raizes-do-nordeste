package br.edu.uninter.raizes.domain.excecao;

public class EstoqueInsuficienteException extends RuntimeException {

    private final int disponivel;

    public EstoqueInsuficienteException(int disponivel) {
        this.disponivel = disponivel;
    }

    public int getDisponivel() {
        return disponivel;
    }
}
