package com.carloslonghi.bcb.infra.worker;

import com.carloslonghi.bcb.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor da fila bcb.messages. Com prefetch = 1 e um consumidor so, processa uma mensagem por vez,
 * e as urgentes que estao esperando passam na frente das normais.
 * Se process() lancar excecao, o Spring tenta de novo (spring.rabbitmq.listener.simple.retry.*) e, esgotadas as
 * tentativas, chama o MessageRecoverer do RabbitConfig, que marca FAILED, estorna e manda para a DLQ.
 */
@Component
@RequiredArgsConstructor
public class RabbitMessageListener {

    private final MessageProcessor messageProcessor;

    @RabbitListener(queues = RabbitConfig.MESSAGES_QUEUE)
    public void onMessage(String messageId) throws InterruptedException {
        messageProcessor.process(Long.valueOf(messageId));
    }
}
