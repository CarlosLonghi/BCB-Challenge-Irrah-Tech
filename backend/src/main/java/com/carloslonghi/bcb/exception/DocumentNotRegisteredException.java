package com.carloslonghi.bcb.exception;

public class DocumentNotRegisteredException extends RuntimeException {

    public DocumentNotRegisteredException() {
        super("Documento não cadastrado. Verifique o CPF/CNPJ.");
    }
}
