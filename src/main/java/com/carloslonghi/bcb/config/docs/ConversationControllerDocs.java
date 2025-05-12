package com.carloslonghi.bcb.config.docs;

import com.carloslonghi.bcb.dto.ConversationDTO;
import com.carloslonghi.bcb.dto.MessageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
public interface ConversationControllerDocs {
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
                            schema = @Schema(implementation = ConversationDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content
            )
    })
    ResponseEntity<List<ConversationDTO>> findAll();

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
                            schema = @Schema(implementation = ConversationDTO.class)
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
    ResponseEntity<ConversationDTO> findById(
            @Parameter(description = "ID da conversa", required = true) @PathVariable Long id
    );

    @Operation(summary = "Listar Mensagens da Conversa", description = "Retorna as mensagens de uma conversa")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Mensagens listadas com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConversationDTO.class)
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
    ResponseEntity<List<MessageDTO>> findMessagesByConversationId(
            @Parameter(description = "ID da conversa", required = true) @PathVariable Long id
    );
}
