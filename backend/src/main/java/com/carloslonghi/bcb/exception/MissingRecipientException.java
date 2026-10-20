package com.carloslonghi.bcb.exception;

/**
 * Envio sem conversationId e sem destinatario completo (recipientId + recipientName). Vira 400.
 */
public class MissingRecipientException extends RuntimeException {

    public MissingRecipientException() {
        super("Para iniciar uma conversa informe recipientId e recipientName.");
    }
}
