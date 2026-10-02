package com.carloslonghi.bcb.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Saldo e limite do cliente: pré-pago usa balance, pós-pago usa limit")
public record ClientBalanceResponse(
        BigDecimal balance,
        BigDecimal limit
) {
}
