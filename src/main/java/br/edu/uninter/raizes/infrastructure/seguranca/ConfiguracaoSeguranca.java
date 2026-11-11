package br.edu.uninter.raizes.infrastructure.seguranca;

import br.edu.uninter.raizes.infrastructure.persistencia.UsuarioRepositorio;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class ConfiguracaoSeguranca {

    @Bean
    public FiltroJwt filtroJwt(EmissorJwt emissorJwt, UsuarioRepositorio usuarios) {
        return new FiltroJwt(emissorJwt, usuarios);
    }

    @Bean
    public FilterRegistrationBean<FiltroJwt> registroDoFiltroJwt(FiltroJwt filtroJwt) {
        FilterRegistrationBean<FiltroJwt> registro = new FilterRegistrationBean<>(filtroJwt);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public SecurityFilterChain cadeiaDeSeguranca(
            HttpSecurity http, FiltroJwt filtroJwt, AuthenticationEntryPoint entradaDeAutenticacao) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .exceptionHandling(tratamento -> tratamento.authenticationEntryPoint(entradaDeAutenticacao))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.POST, "/usuarios").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
