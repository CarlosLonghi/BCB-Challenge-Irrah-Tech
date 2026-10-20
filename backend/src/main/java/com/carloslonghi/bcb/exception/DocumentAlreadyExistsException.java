package com.carloslonghi.bcb.exception;

/**
 * Cadastro com documento ja usado por outro cliente. Vira 409.
 */
public class DocumentAlreadyExistsException extends RuntimeException {

    public DocumentAlreadyExistsException(String document) {
        super("Já existe cliente com o documento " + document + ".");
    }
}
