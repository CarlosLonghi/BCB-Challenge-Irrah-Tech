package com.carloslonghi.bcb.exception;

public class MissingRecipientException extends RuntimeException {

    public MissingRecipientException() {
        super("Para iniciar uma conversa informe recipientId e recipientName.");
    }
}
