package br.edu.uninter.raizes.domain.excecao;

public class ResgateNaoPermitidoException extends RuntimeException {

    private final String mensagem;

    public ResgateNaoPermitidoException(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return mensagem;
    }
}
