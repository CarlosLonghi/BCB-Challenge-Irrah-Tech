package com.carloslonghi.bcb.infra.queue;

import com.carloslonghi.bcb.config.RabbitConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueMetricsTest {

    @Mock
    private AmqpAdmin amqpAdmin;

    @InjectMocks
    private QueueMetrics queueMetrics;

    @Test
    @DisplayName("bcb.queue.messages lê do RabbitMQ as mensagens prontas da fila e da DLQ")
    void readsQueueDepth() {
        when(amqpAdmin.getQueueInfo(RabbitConfig.MESSAGES_QUEUE))
                .thenReturn(new QueueInformation(RabbitConfig.MESSAGES_QUEUE, 4, 1));
        when(amqpAdmin.getQueueInfo(RabbitConfig.DEAD_LETTER_QUEUE))
                .thenReturn(new QueueInformation(RabbitConfig.DEAD_LETTER_QUEUE, 2, 0));
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        queueMetrics.bindTo(registry);

        assertThat(registry.get("bcb.queue.messages").tag("queue", RabbitConfig.MESSAGES_QUEUE).gauge().value()).isEqualTo(4);
        assertThat(registry.get("bcb.queue.messages").tag("queue", RabbitConfig.DEAD_LETTER_QUEUE).gauge().value()).isEqualTo(2);
    }

    @Test
    @DisplayName("fila inexistente ou RabbitMQ fora do ar: sem valor (NaN), sem exceção na coleta")
    void unavailableQueue() {
        when(amqpAdmin.getQueueInfo("inexistente")).thenReturn(null);
        when(amqpAdmin.getQueueInfo("fora")).thenThrow(new AmqpConnectException(new ConnectException("recusada")));

        assertThat(queueMetrics.readyMessages("inexistente")).isNaN();
        assertThat(queueMetrics.readyMessages("fora")).isNaN();
    }
}
