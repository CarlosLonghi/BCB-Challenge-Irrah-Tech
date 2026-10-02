package com.carloslonghi.bcb.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token de acesso e dados do cliente autenticado")
public record AuthResponse(
        @Schema(description = "Token para o header Authorization: Bearer <token>. Guardado em memória: reiniciar o backend invalida", example = "b3f1c0de-8a1e-4c2b-9f00-6d2f8e1a7c55")
        String token,

        ClientResponse client
) {
}
