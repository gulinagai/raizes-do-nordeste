package br.edu.uninter.raizes.api.erro;

import br.edu.uninter.raizes.domain.excecao.CredenciaisInvalidasException;
import br.edu.uninter.raizes.domain.excecao.EmailJaCadastradoException;
import br.edu.uninter.raizes.domain.excecao.GatewayPagamentoIndisponivelException;
import br.edu.uninter.raizes.domain.excecao.PagamentoNaoPermitidoException;
import br.edu.uninter.raizes.domain.excecao.EstoqueInsuficienteException;
import br.edu.uninter.raizes.domain.excecao.ParametrosInvalidosException;
import br.edu.uninter.raizes.domain.excecao.RecursoInativoException;
import br.edu.uninter.raizes.domain.excecao.RecursoNaoEncontradoException;
import br.edu.uninter.raizes.domain.excecao.TransicaoStatusNaoPermitidaException;
import br.edu.uninter.raizes.domain.excecao.ReservaExcedidaException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarValidacao(
            MethodArgumentNotValidException excecao, HttpServletRequest requisicao) {
        List<DetalheErro> detalhes = excecao.getBindingResult().getFieldErrors().stream()
                .map(erro -> new DetalheErro(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return resposta(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "VALIDACAO",
                "Os dados informados são inválidos.",
                detalhes,
                requisicao);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> tratarCorpoInvalido(
            HttpMessageNotReadableException excecao, HttpServletRequest requisicao) {
        return resposta(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "VALIDACAO",
                "Os dados informados são inválidos.",
                List.of(new DetalheErro("corpo", "JSON inválido")),
                requisicao);
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErroResposta> tratarEmailJaCadastrado(
            EmailJaCadastradoException excecao, HttpServletRequest requisicao) {
        return resposta(
                HttpStatus.CONFLICT,
                "EMAIL_JA_CADASTRADO",
                "E-mail já cadastrado.",
                List.of(new DetalheErro("email", "já cadastrado")),
                requisicao);
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ResponseEntity<ErroResposta> tratarEstoqueInsuficiente(
            EstoqueInsuficienteException excecao, HttpServletRequest requisicao) {
        return resposta(
                HttpStatus.CONFLICT,
                "ESTOQUE_INSUFICIENTE",
                "Não há quantidade disponível suficiente.",
                List.of(new DetalheErro("quantidade", "Disponível: " + excecao.getDisponivel())),
                requisicao);
    }

    @ExceptionHandler(ReservaExcedidaException.class)
    public ResponseEntity<ErroResposta> tratarReservaExcedida(
            ReservaExcedidaException excecao, HttpServletRequest requisicao) {
        return resposta(
                HttpStatus.CONFLICT,
                "ESTOQUE_INSUFICIENTE",
                "Não há quantidade suficiente para um ou mais itens.",
                List.of(new DetalheErro(
                        "itens[" + excecao.getIndice() + "].quantidade",
                        "Disponível: " + excecao.getDisponivel())),
                requisicao);
    }

    @ExceptionHandler(RecursoInativoException.class)
    public ResponseEntity<ErroResposta> tratarRecursoInativo(
            RecursoInativoException excecao, HttpServletRequest requisicao) {
        return resposta(
                HttpStatus.CONFLICT,
                "RECURSO_INATIVO",
                "O recurso informado está inativo.",
                List.of(new DetalheErro(excecao.getCampo(), "inativo")),
                requisicao);
    }

    @ExceptionHandler(GatewayPagamentoIndisponivelException.class)
    public ResponseEntity<ErroResposta> tratarGatewayIndisponivel(
            GatewayPagamentoIndisponivelException excecao, HttpServletRequest requisicao) {
        return resposta(
                HttpStatus.SERVICE_UNAVAILABLE,
                "GATEWAY_PAGAMENTO_INDISPONIVEL",
                "O serviço de pagamento está indisponível.",
                List.of(),
                requisicao);
    }

    @ExceptionHandler(TransicaoStatusNaoPermitidaException.class)
    public ResponseEntity<ErroResposta> tratarTransicaoNaoPermitida(
            TransicaoStatusNaoPermitidaException excecao, HttpServletRequest requisicao) {
        return resposta(
                HttpStatus.CONFLICT,
                "TRANSICAO_NAO_PERMITIDA",
                "A transição de status não é permitida.",
                List.of(),
                requisicao);
    }

    @ExceptionHandler(PagamentoNaoPermitidoException.class)
    public ResponseEntity<ErroResposta> tratarPagamentoNaoPermitido(
            PagamentoNaoPermitidoException excecao, HttpServletRequest requisicao) {
        return resposta(
                HttpStatus.CONFLICT,
                "PAGAMENTO_NAO_PERMITIDO",
                "O pedido não está aguardando pagamento.",
                List.of(),
                requisicao);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResposta> tratarCredenciaisInvalidas(
            CredenciaisInvalidasException excecao, HttpServletRequest requisicao) {
        return resposta(
                HttpStatus.UNAUTHORIZED,
                "CREDENCIAIS_INVALIDAS",
                "E-mail ou senha inválidos.",
                List.of(),
                requisicao);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> tratarRecursoNaoEncontrado(
            RecursoNaoEncontradoException excecao, HttpServletRequest requisicao) {
        return resposta(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Recurso não encontrado.", List.of(), requisicao);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResposta> tratarRecursoInexistente(
            NoResourceFoundException excecao, HttpServletRequest requisicao) {
        return resposta(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Recurso não encontrado.", List.of(), requisicao);
    }

    @ExceptionHandler(ParametrosInvalidosException.class)
    public ResponseEntity<ErroResposta> tratarParametrosInvalidos(
            ParametrosInvalidosException excecao, HttpServletRequest requisicao) {
        List<DetalheErro> detalhes = excecao.getCampos().stream()
                .map(campo -> new DetalheErro(campo.nome(), campo.mensagem()))
                .toList();
        return resposta(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "VALIDACAO",
                "Os dados informados são inválidos.",
                detalhes,
                requisicao);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> tratarTipoInvalido(
            MethodArgumentTypeMismatchException excecao, HttpServletRequest requisicao) {
        return resposta(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "VALIDACAO",
                "Os dados informados são inválidos.",
                List.of(new DetalheErro(excecao.getName(), "deve ser um número inteiro")),
                requisicao);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResposta> tratarMetodoNaoPermitido(
            HttpRequestMethodNotSupportedException excecao, HttpServletRequest requisicao) {
        return resposta(
                HttpStatus.METHOD_NOT_ALLOWED, "METODO_NAO_PERMITIDO", "Método não permitido.", List.of(), requisicao);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> tratarErroInesperado(Exception excecao, HttpServletRequest requisicao) {
        log.error("Falha inesperada ao processar {}", requisicao.getRequestURI(), excecao);
        return resposta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ERRO_INTERNO",
                "Ocorreu um erro interno.",
                List.of(),
                requisicao);
    }

    private ResponseEntity<ErroResposta> resposta(
            HttpStatus status,
            String error,
            String message,
            List<DetalheErro> details,
            HttpServletRequest requisicao) {
        return ResponseEntity.status(status)
                .body(ErroResposta.de(error, message, details, requisicao.getRequestURI()));
    }
}
