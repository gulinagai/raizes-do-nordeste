package br.edu.uninter.raizes.infrastructure.seguranca;

import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoJwt {

    static final int TAMANHO_MINIMO_DA_CHAVE = 32;

    @Bean
    public SecretKey chaveJwt(@Value("${jwt.secret}") String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_DA_CHAVE) {
            throw new IllegalStateException(
                    "JWT_SECRET ausente ou curta demais. HS256 exige pelo menos 32 bytes.");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
