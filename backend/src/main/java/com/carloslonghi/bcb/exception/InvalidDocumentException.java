package com.carloslonghi.bcb.exception;

/**
 * Documento incoerente com o tipo (CPF precisa de 11 digitos e CNPJ de 14) ou com digitos
 * verificadores errados. Vira 400.
 */
public class InvalidDocumentException extends RuntimeException {

    public InvalidDocumentException(String documentType, int expectedLength) {
        super(documentType + " deve ter " + expectedLength + " dígitos.");
    }

    public InvalidDocumentException(String documentType) {
        super(documentType + " inválido.");
    }
}
