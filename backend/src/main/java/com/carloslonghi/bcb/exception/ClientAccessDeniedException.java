package com.carloslonghi.bcb.exception;

public class ClientAccessDeniedException extends RuntimeException {

    public ClientAccessDeniedException() {
        super("Acesso permitido somente aos dados do próprio cliente.");
    }
}
