package com.carloslonghi.bcb.controller.response;

import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Detalhes de uma mensagem enviada pelo cliente")
public record MessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        int recipientId,
        String content,

        @Schema(description = "Data/hora de criação, em UTC", example = "2026-09-30T13:45:00.123Z")
        Instant createdAt,

        MessagePriority priority,

        @Schema(description = "Status no ciclo QUEUED -> PROCESSING -> SENT -> DELIVERED (ou FAILED)")
        MessageStatus status,

        BigDecimal cost
) {
}
