package com.carloslonghi.bcb.infra.metrics;

import com.carloslonghi.bcb.entity.enums.MessagePriority;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/**
 * Metricas do ciclo da mensagem, expostas em /actuator/prometheus (porta de gerencia), por prioridade:
 * bcb_messages_sent_total, bcb_messages_delivery_seconds (da criacao ate DELIVERED: espera na fila + envio)
 * e bcb_messages_failed_total.
 * Registradas zeradas na criacao: a serie existe desde o startup, o Grafana mostra 0 em vez de "No data"
 * e o increase() do Prometheus conta o primeiro envio.
 */
@Component
public class MessageMetrics {

    private final Map<MessagePriority, Counter> sent = new EnumMap<>(MessagePriority.class);
    private final Map<MessagePriority, Timer> delivery = new EnumMap<>(MessagePriority.class);
    private final Map<MessagePriority, Counter> failed = new EnumMap<>(MessagePriority.class);

    public MessageMetrics(MeterRegistry registry) {
        for (MessagePriority priority : MessagePriority.values()) {
            sent.put(priority, Counter.builder("bcb.messages.sent")
                    .description("Mensagens enviadas (commit feito)")
                    .tag("priority", priority.name())
                    .register(registry));
            delivery.put(priority, Timer.builder("bcb.messages.delivery")
                    .description("Tempo da criacao ate a entrega (fila + processamento)")
                    .tag("priority", priority.name())
                    .publishPercentileHistogram()
                    .register(registry));
            failed.put(priority, Counter.builder("bcb.messages.failed")
                    .description("Mensagens que esgotaram as tentativas e foram estornadas")
                    .tag("priority", priority.name())
                    .register(registry));
        }
    }

    public void sent(MessagePriority priority) {
        sent.get(priority).increment();
    }

    public void delivered(MessagePriority priority, Duration sinceCreation) {
        delivery.get(priority).record(sinceCreation);
    }

    public void failed(MessagePriority priority) {
        failed.get(priority).increment();
    }
}
