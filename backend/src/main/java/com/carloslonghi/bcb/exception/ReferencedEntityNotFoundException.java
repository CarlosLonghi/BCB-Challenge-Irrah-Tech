package com.carloslonghi.bcb.exception;

/**
 * Entidade nao existe ou pertence a outro cliente (conversa e mensagem de outro contam como
 * inexistentes). Vira 404.
 */
public class ReferencedEntityNotFoundException extends RuntimeException {

    public ReferencedEntityNotFoundException(String entityName, Long id) {
        super(entityName + " de id " + id + " não encontrado(a).");
    }
}
