package com.carloslonghi.bcb.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Contagem de mensagens por status, lida do banco")
public record QueueStatusResponse(
        int queuedTotal,
        int queuedUrgent,
        int queuedNormal,
        int processing,
        int sent,
        int delivered,
        int failed
) {
}
