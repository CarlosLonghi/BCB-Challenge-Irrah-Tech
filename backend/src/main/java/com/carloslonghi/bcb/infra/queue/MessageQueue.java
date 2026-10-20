package com.carloslonghi.bcb.infra.queue;

import com.carloslonghi.bcb.entity.Message;

/**
 * Saida para a fila de processamento assincrono. Implementada por RabbitMessageQueue; mockada nos testes unitarios.
 * Chame so depois do commit da mensagem: o consumidor recebe apenas o id e le o resto do banco.
 */
public interface MessageQueue {

    // Lanca excecao se o broker estiver fora do ar; quem chama decide como tentar de novo (ver PendingMessagesRecovery)
    void enqueue(Message message);
}
