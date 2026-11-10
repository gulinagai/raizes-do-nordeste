package br.edu.uninter.raizes.infrastructure.seguranca;

import br.edu.uninter.raizes.domain.entidade.Usuario;
import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class FiltroJwt extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final EmissorJwt emissorJwt;
    private final UsuarioRepositorio usuarios;

    public FiltroJwt(EmissorJwt emissorJwt, UsuarioRepositorio usuarios) {
        this.emissorJwt = emissorJwt;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        try {
            autenticar(requisicao);
            cadeia.doFilter(requisicao, resposta);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void autenticar(HttpServletRequest requisicao) {
        String cabecalho = requisicao.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho == null || cabecalho.length() <= BEARER.length() || !cabecalho.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return;
        }

        String token = cabecalho.substring(BEARER.length()).trim();
        if (token.isEmpty()) {
            return;
        }

        emissorJwt.identificar(token)
                .flatMap(usuarios::findById)
                .filter(Usuario::isAtivo)
                .ifPresent(this::estabelecer);
    }

    private void estabelecer(Usuario usuario) {
        UsuarioAutenticado autenticado = UsuarioAutenticado.de(usuario);
        var autenticacao = UsernamePasswordAuthenticationToken.authenticated(
                autenticado,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + autenticado.perfil().name())));
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
    }
}
