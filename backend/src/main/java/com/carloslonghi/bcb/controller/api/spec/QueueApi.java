package com.carloslonghi.bcb.controller.api.spec;

import com.carloslonghi.bcb.controller.response.QueueStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Fila")
@SecurityRequirement(name = "bearerAuth")
public interface QueueApi {
    @Operation(
            summary = "Status da Fila",
            description = "Exibe tamanho e contagens por status e prioridade"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Status da fila retornado com sucesso",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = QueueStatusResponse.class)
            )
    )
    ResponseEntity<QueueStatusResponse> getStatus();
}
