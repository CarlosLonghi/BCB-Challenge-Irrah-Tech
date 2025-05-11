package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.dto.QueueStatusDTO;
import com.carloslonghi.bcb.infra.queue.PriorityMessageQueue;
import com.carloslonghi.bcb.model.enums.MessagePriority;
import com.carloslonghi.bcb.model.enums.MessageStatus;
import com.carloslonghi.bcb.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueueStatusService {

    private final PriorityMessageQueue priorityQueue;
    private final MessageRepository messageRepository;

    public QueueStatusDTO getStatus() {
        int queuedTotal = priorityQueue.size();
        int queuedUrgent = priorityQueue.sizeByPriority(MessagePriority.URGENT);
        int queuedNormal = priorityQueue.sizeByPriority(MessagePriority.NORMAL);

        int processing = messageRepository.countByStatus(MessageStatus.PROCESSING);
        int sent = messageRepository.countByStatus(MessageStatus.SENT);
        int delivered = messageRepository.countByStatus(MessageStatus.DELIVERED);
        int failed = messageRepository.countByStatus(MessageStatus.FAILED);

        return new QueueStatusDTO(
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
