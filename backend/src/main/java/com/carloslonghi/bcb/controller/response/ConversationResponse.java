package com.carloslonghi.bcb.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Conversa do cliente com um destinatário")
public record ConversationResponse(
        Long id,
        Long clientId,
        Long recipientId,
        String recipientName,
        @Schema(types = {"string", "null"}, description = "Texto da última mensagem; nulo antes do primeiro envio")
        String lastMessageContent,

        @Schema(types = {"string", "null"}, format = "date-time", description = "Data/hora da última mensagem, em UTC", example = "2026-09-30T13:45:00.123Z")
        Instant lastMessageTime,

        @Schema(description = "Mensagens recebidas ainda não lidas. Sempre 0 por enquanto: o sistema só envia mensagens; passa a valer quando houver recebimento", example = "0")
        int unreadCount
) {
}
