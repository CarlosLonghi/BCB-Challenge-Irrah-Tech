package com.carloslonghi.bcb.config.docs;

import com.carloslonghi.bcb.dto.MessageDTO;
import com.carloslonghi.bcb.dto.MessageRequestDTO;
import com.carloslonghi.bcb.dto.MessageResponseDTO;
import com.carloslonghi.bcb.model.enums.MessageStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Mensagens")
@SecurityRequirement(name = "bearerAuth")
public interface MessageControllerDocs {
    @Operation(
            summary = "Enviar Mensagem",
            description = "Envia nova mensagem em conversa existente ou cria conversa nova"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Mensagem enviada com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Dados inválidos",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content
            )
    })
    ResponseEntity<MessageResponseDTO> send(
            @RequestBody(
                    description = "Dados da mensagem",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = MessageRequestDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Mensagem + nova conversa",
                                            summary = "Sem conversationId → cria conversa nova",
                                            value = """
                                                    {
                                                      "recipientId": 10,
                                                      "recipientName": "Carlos Longhi",
                                                      "content": "Olá, tudo bem?",
                                                      "priority": "NORMAL"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Mensagem em conversa existente",
                                            summary = "Com conversationId → usa conversa já existente",
                                            value = """
                                                    {
                                                      "conversationId": 1,
                                                      "recipientId": 10,
                                                      "content": "Olá, tudo bem?",
                                                      "priority": "NORMAL"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            MessageRequestDTO dto
    );

    @Operation(
            summary = "Listar Mensagens",
            description = "Lista mensagens do usuário autenticado"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Mensagem enviada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content
            )
    })
    ResponseEntity<List<MessageDTO>> findAll();

    @Operation(
            summary = "Buscar Mensagem por ID",
            description = "Obtém detalhes de uma mensagem"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Mensagem encontrada"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Mensagem não encontrada",
                    content = @Content
            )
    })
    ResponseEntity<MessageDTO> findById(
            @Parameter(description = "ID da mensagem", required = true) @PathVariable Long id);

    @Operation(
            summary = "Status da Mensagem",
            description = "Verifica status de uma mensagem"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Status retornado com sucesso"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Mensagem não encontrada",
                    content = @Content
            )
    })
    ResponseEntity<MessageStatus> getStatus(
            @Parameter(description = "ID da mensagem", required = true) @PathVariable Long id);
}
