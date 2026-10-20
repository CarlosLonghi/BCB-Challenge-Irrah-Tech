package com.carloslonghi.bcb.repository;

import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;

/**
 * Linha do GROUP BY status, priority de MessageRepository.countByStatusAndPriority.
 */
public record StatusCount(MessageStatus status, MessagePriority priority, long count) {
}
