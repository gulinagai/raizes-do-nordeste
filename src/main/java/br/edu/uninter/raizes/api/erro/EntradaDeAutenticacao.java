package br.edu.uninter.raizes.api.erro;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class EntradaDeAutenticacao implements AuthenticationEntryPoint {

    private final ObjectMapper json;

    public EntradaDeAutenticacao(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public void commence(
            HttpServletRequest requisicao, HttpServletResponse resposta, AuthenticationException excecao)
            throws IOException {
        if (resposta.isCommitted()) {
            return;
        }
        resposta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(
                resposta.getOutputStream(),
                ErroResposta.de("NAO_AUTENTICADO", "Autenticação necessária.", List.of(), requisicao.getRequestURI()));
    }
}
