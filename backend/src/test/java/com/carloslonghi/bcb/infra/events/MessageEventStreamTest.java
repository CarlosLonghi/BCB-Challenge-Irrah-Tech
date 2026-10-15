package com.carloslonghi.bcb.infra.events;

import com.carloslonghi.bcb.entity.enums.MessageStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MessageEventStreamTest {

    private MessageEventStream stream;

    @BeforeEach
    void setUp() {
        stream = new MessageEventStream();
        ReflectionTestUtils.setField(stream, "timeout", Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("cada aba do cliente vira uma conexão, com o timeout configurado")
    void subscribeRegistersEmitter() {
        SseEmitter first = stream.subscribe(1L);
        stream.subscribe(1L);

        assertThat(first.getTimeout()).isEqualTo(Duration.ofMinutes(30).toMillis());
        assertThat(stream.connections(1L)).isEqualTo(2);
        assertThat(stream.connections(2L)).isZero();
    }

    @Test
    @DisplayName("conexão encerrada sai da lista no próximo envio, sem afetar as outras")
    void completedEmitterIsRemoved() {
        SseEmitter closed = stream.subscribe(1L);
        stream.subscribe(1L);
        closed.complete();

        stream.onStatusChanged(new MessageStatusChanged(1L, 7L, 10L, MessageStatus.SENT));

        assertThat(stream.connections(1L)).isEqualTo(1);
    }

    @Test
    @DisplayName("heartbeat também limpa as conexões encerradas; sem nenhuma, o cliente sai do mapa")
    void heartbeatRemovesDeadConnections() {
        stream.subscribe(1L).complete();

        stream.heartbeat();

        assertThat(stream.connections(1L)).isZero();
    }

    @Test
    @DisplayName("métrica bcb.events.connections soma as abas de todos os clientes")
    void connectionsGauge() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        stream.bindTo(registry);
        stream.subscribe(1L);
        stream.subscribe(1L);
        stream.subscribe(2L);

        assertThat(registry.get("bcb.events.connections").gauge().value()).isEqualTo(3);
    }

    @Test
    @DisplayName("evento de cliente sem conexão aberta é ignorado")
    void eventWithoutConnection() {
        assertThatCode(() -> stream.onStatusChanged(new MessageStatusChanged(9L, 7L, 10L, MessageStatus.SENT)))
                .doesNotThrowAnyException();
    }
}
