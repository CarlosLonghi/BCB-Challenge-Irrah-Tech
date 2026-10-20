package com.carloslonghi.bcb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Beans gerais. {@code @EnableScheduling} liga as rotinas periodicas (PendingMessagesRecovery, ReadSimulator,
 * heartbeat do MessageEventStream).
 */
@Configuration
@EnableScheduling
public class AppConfig {

    // Injetado onde a hora importa (JWT, extrato, recuperacao), para os testes poderem fixar o relogio
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
