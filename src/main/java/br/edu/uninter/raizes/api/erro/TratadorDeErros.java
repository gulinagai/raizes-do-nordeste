package br.edu.uninter.raizes.api.erro;

import br.edu.uninter.raizes.domain.excecao.CredenciaisInvalidasException;
import br.edu.uninter.raizes.domain.excecao.EmailJaCadastradoException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
        ErroResposta corpo = new ErroResposta(
                error,
                message,
                details,
                DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)),
                requisicao.getRequestURI(),
                UUID.randomUUID().toString());
        return ResponseEntity.status(status).body(corpo);
    }
}
