package com.carloslonghi.bcb.controller.api.spec;

import com.carloslonghi.bcb.controller.response.MessageStatusEventResponse;
import com.carloslonghi.bcb.controller.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Eventos")
@SecurityRequirement(name = "bearerAuth")
public interface EventApi {
    @Operation(
            summary = "Eventos em Tempo Real",
            description = """
                    Stream SSE (text/event-stream) com as mudanças de status das mensagens do cliente logado.
                    Eventos: "ready" (conexão aberta, sem dado) e "message-status" (dado em JSON).
                    A conexão fecha depois de 30 minutos; o cliente deve reconectar e recarregar o que estiver na tela.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Stream aberto; cada \"message-status\" traz este JSON",
                    content = @Content(
                            mediaType = "text/event-stream",
                            schema = @Schema(implementation = MessageStatusEventResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    SseEmitter subscribe();
}
