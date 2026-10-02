package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.controller.response.QueueStatusResponse;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueueStatusService {

    private final MessageRepository messageRepository;

    // Conta mensagens pelo banco (status QUEUED)
    public QueueStatusResponse getStatus() {
        int queuedUrgent = messageRepository.countByStatusAndPriority(MessageStatus.QUEUED, MessagePriority.URGENT);
        int queuedNormal = messageRepository.countByStatusAndPriority(MessageStatus.QUEUED, MessagePriority.NORMAL);
        int queuedTotal = queuedUrgent + queuedNormal;

        int processing = messageRepository.countByStatus(MessageStatus.PROCESSING);
        int sent = messageRepository.countByStatus(MessageStatus.SENT);
        int delivered = messageRepository.countByStatus(MessageStatus.DELIVERED);
        int failed = messageRepository.countByStatus(MessageStatus.FAILED);

        return new QueueStatusResponse(
                queuedTotal,
                queuedUrgent,
                queuedNormal,
                processing,
                sent,
                delivered,
                failed
        );
    }
}
