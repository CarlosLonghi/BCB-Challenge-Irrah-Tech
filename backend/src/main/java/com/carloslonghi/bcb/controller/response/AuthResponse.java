package com.carloslonghi.bcb.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token de acesso e dados do cliente autenticado")
public record AuthResponse(
        @Schema(description = "JWT para o header Authorization: Bearer <token>. Vale por 8 horas (bcb.security.jwt.ttl)", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.assinatura")
        String token,

        ClientResponse client
) {
}
