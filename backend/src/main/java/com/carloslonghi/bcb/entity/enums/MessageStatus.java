package com.carloslonghi.bcb.entity.enums;

/**
 * Ciclo da mensagem: QUEUED -> PROCESSING -> SENT -> DELIVERED -> READ, ou FAILED a partir de QUEUED, PROCESSING
 * ou SENT. DELIVERED, READ e FAILED sao finais para o worker. Valor novo exige migration (CHECK em tb_messages.status).
 */
public enum MessageStatus {
    // Gravada e debitada, esperando o worker (ou a publicacao no RabbitMQ, se enqueued_at for nulo)
    QUEUED,
    // O worker pegou a mensagem
    PROCESSING,
    // Enviada ao destinatario (simulado)
    SENT,
    // Entregue; o ReadSimulator passa para READ depois de bcb.simulation.read-after
    DELIVERED,
    // Lida pelo destinatario (simulado)
    READ,
    // Esgotou as tentativas do worker; o custo foi estornado
    FAILED
}
