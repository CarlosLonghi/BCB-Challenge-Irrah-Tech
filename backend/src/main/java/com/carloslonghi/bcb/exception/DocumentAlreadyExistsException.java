package com.carloslonghi.bcb.exception;

public class DocumentAlreadyExistsException extends RuntimeException {

    public DocumentAlreadyExistsException(String document) {
        super("Já existe cliente com o documento " + document + ".");
    }
}
