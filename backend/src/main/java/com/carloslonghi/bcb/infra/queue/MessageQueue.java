package com.carloslonghi.bcb.infra.queue;

import com.carloslonghi.bcb.entity.Message;

public interface MessageQueue {

    // Enfileira uma mensagem para processamento assíncrono.
    void enqueue(Message message);
}
