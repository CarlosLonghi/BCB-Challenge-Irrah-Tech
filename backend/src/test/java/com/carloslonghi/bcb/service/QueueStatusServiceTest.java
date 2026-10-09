package com.carloslonghi.bcb.service;

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
        when(messageRepository.countByStatusAndPriority(MessageStatus.QUEUED, MessagePriority.URGENT)).thenReturn(2);
        when(messageRepository.countByStatusAndPriority(MessageStatus.QUEUED, MessagePriority.NORMAL)).thenReturn(5);
        when(messageRepository.countByStatus(MessageStatus.PROCESSING)).thenReturn(1);
        when(messageRepository.countByStatus(MessageStatus.SENT)).thenReturn(3);
        when(messageRepository.countByStatus(MessageStatus.DELIVERED)).thenReturn(10);
        when(messageRepository.countByStatus(MessageStatus.FAILED)).thenReturn(4);

        assertThat(queueStatusService.getStatus())
                .isEqualTo(new QueueStatusResponse(7, 2, 5, 1, 3, 10, 4));
    }
}
