package com.carloslonghi.bcb.infra.worker;

import com.carloslonghi.bcb.repository.MessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadSimulatorTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");

    @Mock
    private MessageRepository messageRepository;

    @Test
    @DisplayName("marca como lidas as entregues há mais tempo que read-after")
    void marksOldDeliveriesAsRead() {
        ReadSimulator simulator = new ReadSimulator(messageRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(simulator, "readAfter", Duration.ofSeconds(4));
        when(messageRepository.markReadDeliveredBefore(NOW.minusSeconds(4))).thenReturn(3);

        simulator.markDeliveredAsRead();

        verify(messageRepository).markReadDeliveredBefore(NOW.minusSeconds(4));
    }
}
