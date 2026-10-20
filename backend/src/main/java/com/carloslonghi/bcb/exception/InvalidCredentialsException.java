package com.carloslonghi.bcb.exception;

/**
 * Login recusado. Vira 401. Mesma mensagem para documento inexistente e senha errada:
 * nao revela quais documentos estao cadastrados.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Documento ou senha inválidos.");
    }
}
