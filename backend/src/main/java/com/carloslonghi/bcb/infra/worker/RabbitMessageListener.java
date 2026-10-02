package com.carloslonghi.bcb.infra.worker;

import com.carloslonghi.bcb.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// Com prefetch = 1 e um consumidor so, processa uma mensagem por vez.
// Se process() lancar excecao, o Spring tenta de novo (retry) e depois chama o MessageRecoverer.
@Component
@RequiredArgsConstructor
public class RabbitMessageListener {

    private final MessageProcessor messageProcessor;

    @RabbitListener(queues = RabbitConfig.MESSAGES_QUEUE)
    public void onMessage(String messageId) throws InterruptedException {
        messageProcessor.process(Long.valueOf(messageId));
    }
}
