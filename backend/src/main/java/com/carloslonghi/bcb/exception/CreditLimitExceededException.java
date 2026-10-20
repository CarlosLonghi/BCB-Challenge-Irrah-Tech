package com.carloslonghi.bcb.exception;

/** Cliente pós-pago sem limite restante para o custo da mensagem. */
/**
 * Cliente pos-pago sem limite restante para o custo da mensagem. Vira 402.
 */
public class CreditLimitExceededException extends RuntimeException {

    public CreditLimitExceededException() {
        super("Limite de consumo excedido.");
    }
}
