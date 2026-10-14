package com.carloslonghi.bcb.service;

import java.util.List;
import com.carloslonghi.bcb.repository.StatusCount;
import com.carloslonghi.bcb.controller.response.QueueStatusResponse;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.repository.MessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueStatusServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private QueueStatusService queueStatusService;

    @Test
    @DisplayName("monta o status da fila com as contagens do banco; total na fila = urgentes + normais")
    void buildsStatus() {
        when(messageRepository.countByStatusAndPriority()).thenReturn(List.of(
                new StatusCount(MessageStatus.QUEUED, MessagePriority.URGENT, 2),
                new StatusCount(MessageStatus.QUEUED, MessagePriority.NORMAL, 5),
                new StatusCount(MessageStatus.PROCESSING, MessagePriority.URGENT, 1),
                new StatusCount(MessageStatus.SENT, MessagePriority.NORMAL, 1),
                new StatusCount(MessageStatus.SENT, MessagePriority.URGENT, 2),
                new StatusCount(MessageStatus.DELIVERED, MessagePriority.NORMAL, 10),
                new StatusCount(MessageStatus.READ, MessagePriority.NORMAL, 6),
                new StatusCount(MessageStatus.FAILED, MessagePriority.URGENT, 4)));

        assertThat(queueStatusService.getStatus())
                .isEqualTo(new QueueStatusResponse(7, 2, 5, 1, 3, 10, 6, 4));
    }

    @Test
    @DisplayName("status sem nenhuma linha no banco vira tudo zero")
    void emptyDatabase() {
        when(messageRepository.countByStatusAndPriority()).thenReturn(List.of());

        assertThat(queueStatusService.getStatus())
                .isEqualTo(new QueueStatusResponse(0, 0, 0, 0, 0, 0, 0, 0));
    }
}
