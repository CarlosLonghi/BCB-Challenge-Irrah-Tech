package com.carloslonghi.bcb.config;

import com.carloslonghi.bcb.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class RabbitConfig {

    public static final String MESSAGES_QUEUE = "bcb.messages";
    public static final String DEAD_LETTER_QUEUE = "bcb.messages.dlq";
    public static final int MAX_PRIORITY = 10;

    // Fila com prioridade. Mensagem rejeitada vai para a DLQ pelo exchange padrao ("")
    @Bean
    public Queue messagesQueue() {
        return QueueBuilder.durable(MESSAGES_QUEUE)
                .maxPriority(MAX_PRIORITY)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    // Chamado quando as tentativas de spring.rabbitmq.listener.simple.retry acabam:
    // marca a mensagem como FAILED, estorna o custo e rejeita sem reenfileirar, o que a manda para a DLQ
    @Bean
    public MessageRecoverer messageRecoverer(MessageService messageService) {
        return (message, cause) -> {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            log.error("Mensagem {} falhou em todas as tentativas; enviando para a DLQ", body, cause);
            try {
                messageService.failAndRefund(Long.valueOf(body));
            } catch (NumberFormatException e) {
                log.error("Corpo invalido na fila, nao e um id de mensagem: {}", body);
            }
            throw new AmqpRejectAndDontRequeueException("Tentativas esgotadas", cause);
        };
    }
}
