package com.carloslonghi.bcb.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    @DisplayName("segredo curto ou ausente impede a aplicação de subir")
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtProperties("curto", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
        assertThatThrownBy(() -> new JwtProperties(null, Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("sem ttl, o token vale 8 horas")
    void defaultTtl() {
        assertThat(new JwtProperties("segredo-com-mais-de-32-bytes-0123456789", null).ttl())
                .isEqualTo(Duration.ofHours(8));
    }
}
