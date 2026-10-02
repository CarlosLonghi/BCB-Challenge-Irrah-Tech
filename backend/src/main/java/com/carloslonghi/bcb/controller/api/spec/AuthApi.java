package com.carloslonghi.bcb.controller.api.spec;

import com.carloslonghi.bcb.controller.request.AuthRequest;
import com.carloslonghi.bcb.controller.response.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Autenticação")
public interface AuthApi {
    @Operation(summary = "Autenticar Cliente", description = "Gera um token para acesso às demais rotas")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Token gerado com sucesso, junto com os dados do cliente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Documento não cadastrado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Cliente inativo",
                    content = @Content
            )
    })
    ResponseEntity<AuthResponse> authenticate(
            @RequestBody(description = "Documento (CPF ou CNPJ)", required = true)
            AuthRequest request
    );
}
