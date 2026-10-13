package com.carloslonghi.bcb.exception;

// Cobre a coerência com o tipo (CPF precisa de 11 dígitos e CNPJ de 14) e os dígitos verificadores.
public class InvalidDocumentException extends RuntimeException {

    public InvalidDocumentException(String documentType, int expectedLength) {
        super(documentType + " deve ter " + expectedLength + " dígitos.");
    }

    public InvalidDocumentException(String documentType) {
        super(documentType + " inválido.");
    }
}
