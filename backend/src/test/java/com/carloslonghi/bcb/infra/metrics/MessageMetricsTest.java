package com.carloslonghi.bcb.infra.metrics;

import com.carloslonghi.bcb.entity.enums.MessagePriority;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MessageMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MessageMetrics metrics = new MessageMetrics(registry);

    @Test
    @DisplayName("conta envios e falhas por prioridade")
    void countsByPriority() {
        metrics.sent(MessagePriority.NORMAL);
        metrics.sent(MessagePriority.NORMAL);
        metrics.sent(MessagePriority.URGENT);
        metrics.failed(MessagePriority.URGENT);

        assertThat(registry.get("bcb.messages.sent").tag("priority", "NORMAL").counter().count()).isEqualTo(2);
        assertThat(registry.get("bcb.messages.sent").tag("priority", "URGENT").counter().count()).isEqualTo(1);
        assertThat(registry.get("bcb.messages.failed").tag("priority", "URGENT").counter().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("registra o tempo da criação até a entrega")
    void recordsDeliveryTime() {
        metrics.delivered(MessagePriority.URGENT, Duration.ofSeconds(8));
        metrics.delivered(MessagePriority.URGENT, Duration.ofSeconds(12));

        var timer = registry.get("bcb.messages.delivery").tag("priority", "URGENT").timer();
        assertThat(timer.count()).isEqualTo(2);
        assertThat(timer.totalTime(TimeUnit.SECONDS)).isEqualTo(20);
    }
}
