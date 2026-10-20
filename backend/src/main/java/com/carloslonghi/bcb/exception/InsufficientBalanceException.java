package com.carloslonghi.bcb.exception;

/**
 * Cliente pre-pago sem saldo para o custo da mensagem. Vira 402.
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException() {
        super("Saldo insuficiente.");
    }
}
