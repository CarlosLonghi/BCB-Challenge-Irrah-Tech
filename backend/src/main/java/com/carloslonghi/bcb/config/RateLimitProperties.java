package com.carloslonghi.bcb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Limite de tentativas por IP nas rotas publicas.
 *
 * @param enabled  desligado no perfil "it" (os testes criam muitos clientes do mesmo IP)
 * @param auth     POST /auth (forca bruta de senha)
 * @param register POST /clients (cadastro em massa)
 */
@ConfigurationProperties(prefix = "bcb.rate-limit")
public record RateLimitProperties(boolean enabled, Rule auth, Rule register) {

    /** Ate {@code capacity} requisicoes por {@code period}, repostas aos poucos ao longo do periodo. */
    public record Rule(int capacity, Duration period) {
    }
}
