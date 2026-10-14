package com.carloslonghi.bcb.entity.enums;

public enum LedgerEntryType {
    // Custo do envio, descontado do saldo (pre-pago) ou do limite (pos-pago)
    DEBIT,
    // Devolucao do custo de uma mensagem que terminou em FAILED
    REFUND
}
