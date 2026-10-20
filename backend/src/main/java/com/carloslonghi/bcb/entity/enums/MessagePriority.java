package com.carloslonghi.bcb.entity.enums;

/**
 * Prioridade escolhida no envio. Define o custo (MessageService) e a prioridade AMQP (RabbitMessageQueue).
 */
public enum MessagePriority {
    // R$ 0,25, prioridade AMQP 1
    NORMAL,
    // R$ 0,50, prioridade AMQP 10: passa na frente das normais que estao esperando na fila
    URGENT
}
