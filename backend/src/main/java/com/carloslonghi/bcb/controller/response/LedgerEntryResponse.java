package com.carloslonghi.bcb.controller.response;

import com.carloslonghi.bcb.entity.enums.LedgerEntryType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Lançamento do extrato: débito de um envio ou estorno de uma mensagem que falhou")
public record LedgerEntryResponse(
        Long id,

        @Schema(description = "Mensagem que gerou o lançamento", example = "42")
        Long messageId,

        @Schema(description = "DEBIT (saiu do saldo/limite) ou REFUND (voltou)")
        LedgerEntryType type,

        @Schema(description = "Valor em R$, sempre positivo", example = "0.25")
        BigDecimal amount,

        Instant createdAt
) {
}
