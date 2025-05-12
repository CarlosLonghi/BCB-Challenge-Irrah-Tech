package com.carloslonghi.bcb.config.docs;

import com.carloslonghi.bcb.dto.AuthRequest;
import com.carloslonghi.bcb.dto.AuthResponse;
import com.carloslonghi.bcb.dto.ConversationDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Autenticação")
public interface AuthControllerDocs {
    @Operation(summary = "Autenticar Cliente", description = "Gera um token para acesso às demais rotas")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Token gerado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Documento inválido",
                    content = @Content
            )
    })
    ResponseEntity<AuthResponse> authenticate(
            @RequestBody(description = "Documento (CPF ou CNPJ)", required = true)
            AuthRequest request
    );
}
