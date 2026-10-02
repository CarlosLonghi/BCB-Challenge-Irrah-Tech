package com.carloslonghi.bcb.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais para autenticação do cliente")
public record AuthRequest(
        @Schema(description = "CPF ou CNPJ do cliente, só dígitos", example = "12345678901")
        @NotBlank(message = "Documento é obrigatório")
        String document
) {
}
