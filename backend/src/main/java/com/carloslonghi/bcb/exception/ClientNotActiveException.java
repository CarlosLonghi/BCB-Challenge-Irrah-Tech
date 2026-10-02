package com.carloslonghi.bcb.exception;

public class ClientNotActiveException extends RuntimeException {

    public ClientNotActiveException() {
        super("Cliente inativo.");
    }
}
