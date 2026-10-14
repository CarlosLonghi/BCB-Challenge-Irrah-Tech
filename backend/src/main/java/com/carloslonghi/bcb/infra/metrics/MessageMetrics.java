package com.carloslonghi.bcb.infra.metrics;

import com.carloslonghi.bcb.entity.enums.MessagePriority;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Metricas do ciclo da mensagem, expostas em /actuator/prometheus (porta de gerencia), por prioridade:
 * bcb_messages_sent_total, bcb_messages_delivery_seconds (da criacao ate DELIVERED: espera na fila + envio)
 * e bcb_messages_failed_total.
 */
@Component
@RequiredArgsConstructor
public class MessageMetrics {

    private final MeterRegistry registry;

    public void sent(MessagePriority priority) {
        Counter.builder("bcb.messages.sent")
                .description("Mensagens enviadas (commit feito)")
                .tag("priority", priority.name())
                .register(registry)
                .increment();
    }

    public void delivered(MessagePriority priority, Duration sinceCreation) {
        Timer.builder("bcb.messages.delivery")
                .description("Tempo da criacao ate a entrega (fila + processamento)")
                .tag("priority", priority.name())
                .publishPercentileHistogram()
                .register(registry)
                .record(sinceCreation);
    }

    public void failed(MessagePriority priority) {
        Counter.builder("bcb.messages.failed")
                .description("Mensagens que esgotaram as tentativas e foram estornadas")
                .tag("priority", priority.name())
                .register(registry)
                .increment();
    }
}
