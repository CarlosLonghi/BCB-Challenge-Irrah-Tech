package com.carloslonghi.bcb.controller.request;

import com.carloslonghi.bcb.entity.enums.ClientDocumentType;
import com.carloslonghi.bcb.entity.enums.ClientPlanType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Dados para cadastrar um cliente")
public record ClientRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String name,

        @Schema(description = "CPF (11 dígitos) ou CNPJ (14 dígitos), só números; deve combinar com documentType", example = "12345678901")
        @NotBlank(message = "Documento é obrigatório")
        @Pattern(regexp = "\\d{11}|\\d{14}", message = "Documento deve ter 11 (CPF) ou 14 (CNPJ) dígitos, só números")
        String document,

        @NotNull(message = "Tipo de documento é obrigatório (CPF ou CNPJ)")
        ClientDocumentType documentType,

        @NotNull(message = "Plano é obrigatório (PRE_PAID ou POST_PAID)")
        ClientPlanType planType,

        @Schema(description = "Saldo inicial em R$ (usado no pré-pago)", example = "10.00")
        @NotNull(message = "Saldo é obrigatório")
        @PositiveOrZero(message = "Saldo não pode ser negativo")
        BigDecimal balance,

        @Schema(description = "Limite de consumo em R$ (usado no pós-pago)", example = "0.00")
        @NotNull(message = "Limite é obrigatório")
        @PositiveOrZero(message = "Limite não pode ser negativo")
        BigDecimal limit,

        boolean active
) {
}
