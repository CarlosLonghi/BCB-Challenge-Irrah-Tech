package com.carloslonghi.bcb.controller.api.spec;

import com.carloslonghi.bcb.controller.response.ConversationResponse;
import com.carloslonghi.bcb.controller.response.MessageResponse;
import com.carloslonghi.bcb.controller.response.ErrorResponse;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Conversas")
@SecurityRequirement(name = "bearerAuth")
public interface ConversationApi {
    @Operation(
            summary = "Listar Conversas",
            description = "Conversas do cliente autenticado, da última mensagem mais recente para a mais antiga, paginadas por page/size"
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
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<List<ConversationResponse>> findAll(
            @Parameter(description = "Página, começando em 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Conversas por página: padrão 50, ajustado para 1..100") @RequestParam(defaultValue = "50") int size
    );

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
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Conversa não encontrada (ou de outro cliente)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ConversationResponse> findById(
            @Parameter(description = "ID da conversa", required = true) @PathVariable Long id
    );

    @Operation(
            summary = "Listar Mensagens da Conversa",
            description = "As mensagens mais recentes da conversa (ou as anteriores ao cursor), em ordem crescente de envio"
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
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Conversa não encontrada (ou de outro cliente)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<List<MessageResponse>> findMessagesByConversationId(
            @Parameter(description = "ID da conversa", required = true) @PathVariable Long id,
            @Parameter(description = "Cursor: só itens com id menor que este; omitir para começar dos mais recentes") @RequestParam(required = false) Long before,
            @Parameter(description = "Itens por página: padrão 50, ajustado para 1..100") @RequestParam(defaultValue = "50") int limit
    );
}
