package com.carloslonghi.bcb.infra.queue;

import com.carloslonghi.bcb.config.RabbitConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * bcb_queue_messages{queue=...}: mensagens prontas (ainda nao entregues ao consumidor) na fila principal
 * e na DLQ, lidas do RabbitMQ a cada coleta. A que esta sendo processada nao entra na conta.
 */
@Component
@RequiredArgsConstructor
public class QueueMetrics implements MeterBinder {

    private final AmqpAdmin amqpAdmin;

    @Override
    public void bindTo(MeterRegistry registry) {
        for (String queue : List.of(RabbitConfig.MESSAGES_QUEUE, RabbitConfig.DEAD_LETTER_QUEUE)) {
            Gauge.builder("bcb.queue.messages", () -> readyMessages(queue))
                    .description("Mensagens esperando na fila")
                    .tag("queue", queue)
                    .register(registry);
        }
    }

    // RabbitMQ fora do ar ou fila inexistente: NaN, que o Prometheus mostra como "sem valor"
    double readyMessages(String queue) {
        try {
            QueueInformation info = amqpAdmin.getQueueInfo(queue);
            return info == null ? Double.NaN : info.getMessageCount();
        } catch (AmqpException e) {
            return Double.NaN;
        }
    }
}
