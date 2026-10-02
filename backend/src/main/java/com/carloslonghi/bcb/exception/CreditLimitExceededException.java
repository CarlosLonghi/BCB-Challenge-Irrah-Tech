package com.carloslonghi.bcb.exception;

/** Cliente pós-pago sem limite restante para o custo da mensagem. */
public class CreditLimitExceededException extends RuntimeException {

    public CreditLimitExceededException() {
        super("Limite de consumo excedido.");
    }
}
