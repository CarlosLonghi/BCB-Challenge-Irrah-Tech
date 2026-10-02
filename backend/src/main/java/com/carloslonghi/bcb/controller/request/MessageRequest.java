package com.carloslonghi.bcb.controller.request;

import com.carloslonghi.bcb.entity.enums.MessagePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Mensagem a enviar. Conversa existente: informe conversationId. Nova conversa: omita conversationId e informe recipientId e recipientName")
public record MessageRequest(
        @Schema(description = "ID da conversa existente; omitir para iniciar uma nova", example = "7")
        Long conversationId,

        @Schema(description = "ID do destinatário; obrigatório só ao iniciar conversa", example = "55")
        Integer recipientId,

        @Schema(description = "Nome do destinatário; obrigatório só ao iniciar conversa", example = "Maria Souza")
        @Size(max = 255, message = "Nome do destinatário deve ter no máximo 255 caracteres")
        String recipientName,

        @NotBlank(message = "Mensagem não pode ser vazia")
        @Size(max = 255, message = "Mensagem deve ter no máximo 255 caracteres")
        String content,

        @Schema(description = "Prioridade: NORMAL custa R$ 0,25 e URGENT R$ 0,50 (processada antes)")
        @NotNull(message = "Prioridade é obrigatória (NORMAL ou URGENT)")
        MessagePriority priority
) {
}
