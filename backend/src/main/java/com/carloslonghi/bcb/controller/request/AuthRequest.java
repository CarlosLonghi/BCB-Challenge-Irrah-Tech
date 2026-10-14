package com.carloslonghi.bcb.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais do cliente: documento e senha")
public record AuthRequest(
        @Schema(description = "CPF ou CNPJ do cliente, só dígitos", example = "12345678909")
        @NotBlank(message = "Documento é obrigatório")
        String document,

        @Schema(description = "Senha cadastrada", example = "minha-senha-123")
        @NotBlank(message = "Senha é obrigatória")
        String password
) {
}
