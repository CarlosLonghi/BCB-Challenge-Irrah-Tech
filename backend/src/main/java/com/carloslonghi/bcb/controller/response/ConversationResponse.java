package com.carloslonghi.bcb.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Conversa do cliente com um destinatário")
public record ConversationResponse(
        Long id,
        Long clientId,
        int recipientId,
        String recipientName,
        String lastMessageContent,

        @Schema(description = "Data/hora da última mensagem, em UTC", example = "2026-09-30T13:45:00.123Z")
        Instant lastMessageTime,

        @Schema(description = "Mensagens recebidas ainda não lidas. Sempre 0: Nesse momento, o sistema só envia mensagens e não recebe respostas.", example = "0")
        int unreadCount
) {
}
