package com.carloslonghi.bcb.exception;

public class ReferencedEntityNotFoundException extends RuntimeException {

    public ReferencedEntityNotFoundException(String entityName, Long id) {
        super(entityName + " de id " + id + " não encontrado(a).");
    }
}
