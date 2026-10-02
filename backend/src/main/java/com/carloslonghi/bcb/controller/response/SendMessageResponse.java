package com.carloslonghi.bcb.controller.response;

import com.carloslonghi.bcb.entity.enums.MessageStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Resultado do envio de uma mensagem")
public record SendMessageResponse(
        Long id,

        @Schema(description = "ID da conversa usada ou criada; ao iniciar conversa, usar para abrir o chat", example = "7")
        Long conversationId,

        @Schema(description = "Sempre QUEUED: a mensagem entra na fila depois que o débito é gravado")
        MessageStatus status,

        @Schema(description = "Previsão de entrega em UTC (criação + 30 s)", example = "2026-09-30T13:45:30Z")
        Instant estimatedDelivery,

        BigDecimal cost,

        @Schema(description = "Pré-pago: saldo após o débito; pós-pago: limite restante", example = "8.75")
        BigDecimal currentBalance
) {
}
