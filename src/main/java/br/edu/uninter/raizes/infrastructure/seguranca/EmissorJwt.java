package br.edu.uninter.raizes.infrastructure.seguranca;

import br.edu.uninter.raizes.domain.entidade.Usuario;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmissorJwt {

    private final SecretKey chave;
    private final long expiracaoSegundos;

    public EmissorJwt(SecretKey chave, @Value("${jwt.expiration-seconds}") long expiracaoSegundos) {
        if (expiracaoSegundos <= 0) {
            throw new IllegalStateException("JWT_EXPIRATION_SECONDS deve ser maior que zero.");
        }
        this.chave = chave;
        this.expiracaoSegundos = expiracaoSegundos;
    }

    public String emitir(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusSeconds(expiracaoSegundos)))
                .signWith(chave)
                .compact();
    }

    public long expiracaoSegundos() {
        return expiracaoSegundos;
    }

    public Optional<Long> identificar(String token) {
        try {
            String sujeito = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            if (sujeito == null || sujeito.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(Long.valueOf(sujeito));
        } catch (JwtException | IllegalArgumentException excecao) {
            return Optional.empty();
        }
    }
}
