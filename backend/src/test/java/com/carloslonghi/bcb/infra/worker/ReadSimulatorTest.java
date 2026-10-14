package com.carloslonghi.bcb.infra.worker;

import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.infra.events.MessageStatusChanged;
import com.carloslonghi.bcb.repository.MessageRef;
import com.carloslonghi.bcb.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadSimulatorTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private static final Instant CUTOFF = NOW.minusSeconds(4);
    private static final Limit BATCH = Limit.of(ReadSimulator.BATCH_SIZE);

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ApplicationEventPublisher events;

    private ReadSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new ReadSimulator(messageRepository, Clock.fixed(NOW, ZoneOffset.UTC), events);
        ReflectionTestUtils.setField(simulator, "readAfter", Duration.ofSeconds(4));
    }

    @Test
    @DisplayName("marca como lidas as entregues há mais tempo que read-after e avisa o dono de cada uma")
    void marksOldDeliveriesAsRead() {
        when(messageRepository.findDeliveredBefore(CUTOFF, BATCH))
                .thenReturn(List.of(new MessageRef(10L, 1L, 7L), new MessageRef(11L, 2L, 8L)));

        simulator.markDeliveredAsRead();

        InOrder inOrder = inOrder(messageRepository, events);
        inOrder.verify(messageRepository).markRead(List.of(10L, 11L));
        inOrder.verify(events).publishEvent(new MessageStatusChanged(1L, 7L, 10L, MessageStatus.READ));
        inOrder.verify(events).publishEvent(new MessageStatusChanged(2L, 8L, 11L, MessageStatus.READ));
    }

    @Test
    @DisplayName("nada entregue há tempo suficiente: não atualiza nem avisa")
    void nothingToRead() {
        when(messageRepository.findDeliveredBefore(CUTOFF, BATCH)).thenReturn(List.of());

        simulator.markDeliveredAsRead();

        verify(messageRepository, never()).markRead(any());
        verifyNoInteractions(events);
    }
}
