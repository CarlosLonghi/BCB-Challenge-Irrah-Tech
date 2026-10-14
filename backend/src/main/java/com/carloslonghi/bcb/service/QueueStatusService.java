package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.controller.response.QueueStatusResponse;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.repository.MessageRepository;
import com.carloslonghi.bcb.repository.StatusCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QueueStatusService {

    private final MessageRepository messageRepository;

    // Conta mensagens pelo banco, numa consulta so (GROUP BY status, priority)
    public QueueStatusResponse getStatus() {
        Map<MessageStatus, Map<MessagePriority, Long>> counts = new EnumMap<>(MessageStatus.class);
        for (StatusCount row : messageRepository.countByStatusAndPriority()) {
            counts.computeIfAbsent(row.status(), status -> new EnumMap<>(MessagePriority.class))
                    .put(row.priority(), row.count());
        }

        int queuedUrgent = count(counts, MessageStatus.QUEUED, MessagePriority.URGENT);
        int queuedNormal = count(counts, MessageStatus.QUEUED, MessagePriority.NORMAL);

        return new QueueStatusResponse(
                queuedUrgent + queuedNormal,
                queuedUrgent,
                queuedNormal,
                total(counts, MessageStatus.PROCESSING),
                total(counts, MessageStatus.SENT),
                total(counts, MessageStatus.DELIVERED),
                total(counts, MessageStatus.READ),
                total(counts, MessageStatus.FAILED)
        );
    }

    private static int count(Map<MessageStatus, Map<MessagePriority, Long>> counts, MessageStatus status, MessagePriority priority) {
        return counts.getOrDefault(status, Map.of()).getOrDefault(priority, 0L).intValue();
    }

    private static int total(Map<MessageStatus, Map<MessagePriority, Long>> counts, MessageStatus status) {
        return counts.getOrDefault(status, Map.of()).values().stream().mapToInt(Long::intValue).sum();
    }
}
