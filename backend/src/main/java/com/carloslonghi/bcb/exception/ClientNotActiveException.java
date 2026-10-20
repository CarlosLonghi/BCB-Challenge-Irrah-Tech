package com.carloslonghi.bcb.exception;

/**
 * Login de cliente inativo, com a senha certa. Vira 403.
 */
public class ClientNotActiveException extends RuntimeException {

    public ClientNotActiveException() {
        super("Cliente inativo.");
    }
}
