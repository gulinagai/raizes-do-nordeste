package br.edu.uninter.raizes.infrastructure.seguranca;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoOpenApi {

    @Bean
    public OpenAPI raizesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Raízes do Nordeste")
                        .description("""
                                API da rede de restaurantes. Uma falha inesperada responde 500 \
                                com o corpo ErroResposta e o código ERRO_INTERNO.""")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description(
                                        "Informe somente o accessToken. A interface adiciona o prefixo Bearer.")));
    }
}
