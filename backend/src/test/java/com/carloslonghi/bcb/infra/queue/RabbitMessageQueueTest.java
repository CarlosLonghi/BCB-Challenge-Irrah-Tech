package com.carloslonghi.bcb.infra.queue;

import com.carloslonghi.bcb.config.RabbitConfig;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static com.carloslonghi.bcb.support.TestData.conversation;
import static com.carloslonghi.bcb.support.TestData.message;
import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMessageQueueTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMessageQueue messageQueue;

    @ParameterizedTest(name = "{0} vai com prioridade {1}")
    @CsvSource({"URGENT, " + RabbitConfig.MAX_PRIORITY, "NORMAL, 1"})
    @DisplayName("publica só o id da mensagem, com a prioridade do Rabbit pela prioridade da mensagem")
    void publishesIdWithPriority(MessagePriority priority, int expectedPriority) {
        Message message = message(42L, conversation(7L, prePaidClient(1L, "1.00")), priority, MessageStatus.QUEUED);

        messageQueue.enqueue(message);

        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.MESSAGES_QUEUE), eq((Object) "42"), captor.capture());

        org.springframework.amqp.core.Message amqpMessage =
                new org.springframework.amqp.core.Message(new byte[0], new MessageProperties());
        captor.getValue().postProcessMessage(amqpMessage);
        assertThat(amqpMessage.getMessageProperties().getPriority()).isEqualTo(expectedPriority);
    }

    @Test
    @DisplayName("RabbitMQ fora do ar: a exceção propaga (o MessageService trata e a recuperação reenfileira)")
    void brokerDownPropagates() {
        Message message = message(42L, conversation(7L, prePaidClient(1L, "1.00")), MessagePriority.NORMAL, MessageStatus.QUEUED);
        doThrow(new AmqpConnectException(new java.net.ConnectException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(eq(RabbitConfig.MESSAGES_QUEUE), eq((Object) "42"), any(MessagePostProcessor.class));

        assertThatThrownBy(() -> messageQueue.enqueue(message))
                .isInstanceOf(AmqpConnectException.class);
    }
}
