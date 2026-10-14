package com.carloslonghi.bcb.controller.response;

import com.carloslonghi.bcb.entity.enums.MessageStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dado do evento SSE \"message-status\": a mensagem mudou de status")
public record MessageStatusEventResponse(
        Long messageId,
        Long conversationId,
        MessageStatus status
) {
}
