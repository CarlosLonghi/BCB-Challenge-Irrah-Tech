package com.carloslonghi.bcb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuracao do JWT de sessao.
 *
 * @param secret chave HMAC-SHA256, com pelo menos 32 bytes (variavel JWT_SECRET)
 * @param ttl    validade do token depois do login
 */
@ConfigurationProperties(prefix = "bcb.security.jwt")
public record JwtProperties(String secret, Duration ttl) {

    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes().length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("bcb.security.jwt.secret (JWT_SECRET) precisa ter pelo menos "
                    + MIN_SECRET_BYTES + " bytes");
        }
        if (ttl == null) {
            ttl = Duration.ofHours(8);
        }
    }
}
