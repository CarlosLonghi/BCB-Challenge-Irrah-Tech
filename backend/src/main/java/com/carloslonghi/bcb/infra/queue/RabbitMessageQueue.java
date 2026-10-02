package com.carloslonghi.bcb.infra.queue;

import com.carloslonghi.bcb.config.RabbitConfig;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

// Publica so o id da mensagem. O consumidor le o resto do banco
@Component
@RequiredArgsConstructor
public class RabbitMessageQueue implements MessageQueue {

    private static final int URGENT_PRIORITY = RabbitConfig.MAX_PRIORITY;
    private static final int NORMAL_PRIORITY = 1;

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void enqueue(Message message) {
        int priority = message.getPriority() == MessagePriority.URGENT ? URGENT_PRIORITY : NORMAL_PRIORITY;

        rabbitTemplate.convertAndSend(RabbitConfig.MESSAGES_QUEUE, message.getId().toString(), amqpMessage -> {
            amqpMessage.getMessageProperties().setPriority(priority);
            return amqpMessage;
        });
    }
}
