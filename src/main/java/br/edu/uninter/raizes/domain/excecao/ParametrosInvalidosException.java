package br.edu.uninter.raizes.domain.excecao;

import java.util.List;

public class ParametrosInvalidosException extends RuntimeException {

    private final List<Campo> campos;

    public ParametrosInvalidosException(List<Campo> campos) {
        this.campos = List.copyOf(campos);
    }

    public List<Campo> getCampos() {
        return campos;
    }

    public record Campo(String nome, String mensagem) {
    }
}
