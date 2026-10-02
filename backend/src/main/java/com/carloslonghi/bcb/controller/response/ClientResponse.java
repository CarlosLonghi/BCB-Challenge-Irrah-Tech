package com.carloslonghi.bcb.controller.response;

import com.carloslonghi.bcb.entity.enums.ClientDocumentType;
import com.carloslonghi.bcb.entity.enums.ClientPlanType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Detalhes de um cliente retornado pela API")
public record ClientResponse(
        Long id,
        String name,
        String document,
        ClientDocumentType documentType,
        ClientPlanType planType,

        @Schema(description = "Saldo atual em R$ (pré-pago)", example = "8.75")
        BigDecimal balance,

        @Schema(description = "Limite restante em R$ (pós-pago)", example = "0.00")
        BigDecimal limit,

        boolean active
) {
}
