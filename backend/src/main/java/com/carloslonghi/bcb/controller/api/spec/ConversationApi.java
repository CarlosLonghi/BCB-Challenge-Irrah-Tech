package com.carloslonghi.bcb.controller.api.spec;

import com.carloslonghi.bcb.controller.response.ConversationResponse;
import com.carloslonghi.bcb.controller.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Conversas")
@SecurityRequirement(name = "bearerAuth")
public interface ConversationApi {
    @Operation(
            summary = "Listar Conversas",
            description = "Retorna todas as conversas do cliente autenticado"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Conversas listadas com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(
                                schema = @Schema(implementation = ConversationResponse.class)
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content
            )
    })
    ResponseEntity<List<ConversationResponse>> findAll();

    @Operation(
            summary = "Obter Conversa por ID",
            description = "Detalhes de uma conversa específica"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Conversa encontrada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConversationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Conversa não encontrada",
                    content = @Content
            )
    })
    ResponseEntity<ConversationResponse> findById(
            @Parameter(description = "ID da conversa", required = true) @PathVariable Long id
    );

    @Operation(
            summary = "Listar Mensagens da Conversa",
            description = "Retorna as mensagens de uma conversa"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Mensagens listadas com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(
                                schema = @Schema(implementation = MessageResponse.class)
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Conversa não encontrada",
                    content = @Content
            )
    })
    ResponseEntity<List<MessageResponse>> findMessagesByConversationId(
            @Parameter(description = "ID da conversa", required = true) @PathVariable Long id
    );
}
