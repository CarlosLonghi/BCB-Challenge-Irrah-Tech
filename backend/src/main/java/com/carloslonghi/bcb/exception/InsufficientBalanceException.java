package com.carloslonghi.bcb.exception;

// Cliente pré-pago sem saldo para o custo da mensagem.
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException() {
        super("Saldo insuficiente.");
    }
}
