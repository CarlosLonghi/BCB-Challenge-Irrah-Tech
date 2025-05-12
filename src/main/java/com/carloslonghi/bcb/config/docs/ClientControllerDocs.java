package com.carloslonghi.bcb.config.docs;

import com.carloslonghi.bcb.dto.ClientBalanceDTO;
import com.carloslonghi.bcb.dto.ClientDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

@Tag(name = "Clientes")
@SecurityRequirement(name = "bearerAuth")
public interface ClientControllerDocs {
    @Operation(
            summary = "Criar Cliente",
            description = "Cadastro de novo cliente (PF ou PJ) - (Rota Pública)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Cliente criado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ClientDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Dados inválidos",
                    content = @Content
            )
    })
    ResponseEntity<ClientDTO> create(
            @RequestBody(
                    description = "Dados do cliente",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ClientDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Cliente pré-pago",
                                            value = """
                                                    {
                                                      "name": "Empresa Exemplo 1",
                                                      "document": "12345678000190",
                                                      "documentType": "CNPJ",
                                                      "planType": "PRE_PAID",
                                                      "balance": 100.0,
                                                      "limit": 0.0,
                                                      "active": true
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Cliente pós-pago",
                                            value = """
                                                    {
                                                      "name": "Empresa Exemplo 2",
                                                      "document": "12345678000190",
                                                      "documentType": "CNPJ",
                                                      "planType": "POST_PAID",
                                                      "balance": 0.0,
                                                      "limit": 50.0,
                                                      "active": true
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            ClientDTO dto
    );

    @Operation(
            summary = "Listar Clientes",
            description = "Lista todos os clientes (somente admin)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Clientes listados com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(
                                    schema = @Schema(implementation = ClientDTO.class)
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content
            )
    })
    ResponseEntity<List<ClientDTO>> findAll();

    @Operation(
            summary = "Obter Cliente por ID",
            description = "Busca um cliente existente pelo ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cliente encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ClientDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Cliente não encontrado",
                    content = @Content
            )
    })
    ResponseEntity<ClientDTO> findById(
            @Parameter(description = "ID do cliente", required = true) @PathVariable Long id
    );

    @Operation(
            summary = "Atualizar Cliente por ID",
            description = "Atualiza dados de um cliente existente"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cliente atualizado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ClientDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Cliente não encontrado",
                    content = @Content
            )
    })
    ResponseEntity<ClientDTO> updateById(
            @Parameter(description = "ID do cliente", required = true) @PathVariable Long id,
            @RequestBody(
                    description = "Dados do cliente",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ClientDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Cliente pré-pago",
                                            value = """
                                                    {
                                                      "name": "Empresa Exemplo",
                                                      "document": "12345678000190",
                                                      "documentType": "CNPJ",
                                                      "planType": "PRE_PAID",
                                                      "balance": 100.0,
                                                      "limit": 0.0,
                                                      "active": true
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            ClientDTO dto
    );

    @Operation(
            summary = "Consultar Saldo/Limite",
            description = "Retorna saldo (pré-pago) ou limite (pós-pago)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Saldo/limite retornado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ClientBalanceDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Cliente não encontrado",
                    content = @Content
            )
    })
    ResponseEntity<ClientBalanceDTO> getClientBalance(
            @Parameter(description = "ID do cliente", required = true) @PathVariable Long id
    );
}
