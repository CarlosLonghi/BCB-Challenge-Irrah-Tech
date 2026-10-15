package com.carloslonghi.bcb.controller.api.spec;

import com.carloslonghi.bcb.controller.request.ClientRequest;
import com.carloslonghi.bcb.controller.request.ClientUpdateRequest;
import com.carloslonghi.bcb.controller.response.ClientBalanceResponse;
import com.carloslonghi.bcb.controller.response.LedgerEntryResponse;
import com.carloslonghi.bcb.controller.response.ClientResponse;
import com.carloslonghi.bcb.controller.response.ErrorResponse;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Clientes")
public interface ClientApi {
    @Operation(
            summary = "Criar Cliente",
            description = "Cadastro de novo cliente (PF ou PJ) com a senha de acesso. Saldo e limite são definidos só aqui - (Rota Pública)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Cliente criado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ClientResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos (campo obrigatório ausente, senha fora de 8 a 72 caracteres, documento inválido ou que não combina com documentType)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Documento já cadastrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Muitos cadastros do mesmo IP; aguarde o tempo do header Retry-After",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ClientResponse> create(
            @RequestBody(
                    description = "Dados do cliente",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ClientRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Cliente pré-pago",
                                            value = """
                                                    {
                                                      "name": "Empresa Exemplo 1",
                                                      "document": "11222333000181",
                                                      "password": "minha-senha-123",
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
                                                      "document": "11444777000161",
                                                      "password": "minha-senha-123",
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
            ClientRequest request
    );

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Listar Clientes",
            description = "Retorna uma lista só com o cliente autenticado"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista com o cliente autenticado",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(
                                    schema = @Schema(implementation = ClientResponse.class)
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<List<ClientResponse>> findAll();

    @SecurityRequirement(name = "bearerAuth")
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
                            schema = @Schema(implementation = ClientResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "ID de outro cliente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ClientResponse> findById(
            @Parameter(description = "ID do cliente", required = true) @PathVariable Long id
    );

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Atualizar Cliente por ID",
            description = "Atualiza o nome do próprio cliente. Saldo, limite, plano e documento não são alterados por esta rota"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cliente atualizado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ClientResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nome ausente ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "ID de outro cliente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ClientResponse> updateById(
            @Parameter(description = "ID do cliente", required = true) @PathVariable Long id,
            @RequestBody(
                    description = "Novo nome do cliente",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ClientUpdateRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Alterar nome",
                                            value = """
                                                    {
                                                      "name": "Empresa Exemplo Ltda"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            ClientUpdateRequest request
    );

    @SecurityRequirement(name = "bearerAuth")
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
                            schema = @Schema(implementation = ClientBalanceResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "ID de outro cliente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ClientBalanceResponse> getClientBalance(
            @Parameter(description = "ID do cliente", required = true) @PathVariable Long id
    );

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Consultar Extrato",
            description = "Débitos de envio e estornos de mensagens que falharam, dos mais recentes para os mais antigos"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Extrato retornado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = LedgerEntryResponse.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Extrato de outro cliente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<List<LedgerEntryResponse>> getLedger(
            @Parameter(description = "ID do cliente", required = true) @PathVariable Long id,
            @Parameter(description = "Cursor: só itens com id menor que este; omitir para começar dos mais recentes") @RequestParam(required = false) Long before,
            @Parameter(description = "Itens por página: padrão 50, ajustado para 1..100") @RequestParam(defaultValue = "50") int limit
    );
}
