package br.edu.uninter.raizes.api.erro;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class AcessoNegado implements AccessDeniedHandler {

    private final ObjectMapper json;

    public AcessoNegado(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public void handle(
            HttpServletRequest requisicao, HttpServletResponse resposta, AccessDeniedException excecao)
            throws IOException {
        if (resposta.isCommitted()) {
            return;
        }
        resposta.setStatus(HttpServletResponse.SC_FORBIDDEN);
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(
                resposta.getOutputStream(),
                ErroResposta.de("ACESSO_NEGADO", "Acesso negado.", List.of(), requisicao.getRequestURI()));
    }
}
