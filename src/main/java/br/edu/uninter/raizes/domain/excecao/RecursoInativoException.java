package br.edu.uninter.raizes.domain.excecao;

public class RecursoInativoException extends RuntimeException {

    private final String campo;

    public RecursoInativoException(String campo) {
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
