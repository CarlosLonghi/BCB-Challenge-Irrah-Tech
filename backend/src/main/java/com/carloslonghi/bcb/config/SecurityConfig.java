package com.carloslonghi.bcb.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Cadeia de seguranca stateless: CORS, rate limit, rotas publicas e validacao do JWT (oauth2ResourceServer).
 * Rota publica nova entra so aqui. Depois da autenticacao, o principal e o clientId (Long), que os services
 * leem do SecurityContextHolder para filtrar os dados do cliente logado.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(RateLimitProperties.class)
public class SecurityConfig {

    @Value("${bcb.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RateLimitProperties rateLimit) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Depois do CORS: o 429 sai com Access-Control-Allow-Origin e o navegador consegue ler a mensagem
                .addFilterAfter(new RateLimitFilter(rateLimit), CorsFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Fim de resposta assincrona (SSE do /events): a requisicao original ja foi autorizada,
                        // e o dispatch ASYNC nao traz o token de novo (sessao stateless)
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        // Erros fora dos controllers (ex.: 401 de token invalido) vao para /error;
                        // os dos controllers sao tratados no ApplicationControllerAdvice
                        .requestMatchers("/error").permitAll()
                        // Actuator fica na porta de gerencia (8081), que o compose nao publica
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth").permitAll()
                        .requestMatchers(HttpMethod.POST, "/clients").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // O Bearer e um JWT emitido pelo AuthService; o id do cliente vira o principal
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(clientIdPrincipal()))
                        .authenticationEntryPoint(unauthorized()))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(unauthorized()))
                .build();
    }

    // Os services leem o principal como Long: (Long) authentication.getPrincipal()
    private Converter<Jwt, AbstractAuthenticationToken> clientIdPrincipal() {
        return jwt -> new UsernamePasswordAuthenticationToken(Long.valueOf(jwt.getSubject()), jwt, List.of());
    }

    // 401 pelo /error do Spring Boot, no mesmo formato JSON do ErrorResponse
    private AuthenticationEntryPoint unauthorized() {
        return (request, response, exception) -> {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            boolean sentToken = header != null && header.regionMatches(true, 0, "Bearer ", 0, 7);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, sentToken
                    ? "Sessão inválida ou expirada. Faça login novamente."
                    : "Sessão não encontrada. Faça login para continuar.");
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of(TraceIdHeaderFilter.HEADER, HttpHeaders.RETRY_AFTER));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
