package com.carloslonghi.bcb.exception;

/**
 * Cliente tentou acessar dados de outro cliente por id (/clients/{id}...). Vira 403.
 */
public class ClientAccessDeniedException extends RuntimeException {

    public ClientAccessDeniedException() {
        super("Acesso permitido somente aos dados do próprio cliente.");
    }
}
