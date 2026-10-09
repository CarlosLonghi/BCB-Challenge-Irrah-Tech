package com.carloslonghi.bcb.mapper;

import com.carloslonghi.bcb.controller.response.MessageResponse;
import com.carloslonghi.bcb.controller.response.SendMessageResponse;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static com.carloslonghi.bcb.support.TestData.conversation;
import static com.carloslonghi.bcb.support.TestData.message;
import static com.carloslonghi.bcb.support.TestData.postPaidClient;
import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.assertj.core.api.Assertions.assertThat;

class MessageMapperTest {

    private final MessageMapper mapper = Mappers.getMapper(MessageMapper.class);

    private Message messageFrom(Client sender) {
        return message(1L, conversation(7L, sender), MessagePriority.URGENT, MessageStatus.QUEUED);
    }

    @Test
    @DisplayName("no pré-pago, currentBalance é o saldo")
    void prePaidShowsBalance() {
        SendMessageResponse response = mapper.toSendResponse(messageFrom(prePaidClient(1L, "9.50")));

        assertThat(response.currentBalance()).isEqualByComparingTo("9.50");
    }

    @Test
    @DisplayName("no pós-pago, currentBalance é o limite restante")
    void postPaidShowsLimit() {
        SendMessageResponse response = mapper.toSendResponse(messageFrom(postPaidClient(1L, "4.75")));

        assertThat(response.currentBalance()).isEqualByComparingTo("4.75");
    }

    @Test
    @DisplayName("toSendResponse estima a entrega 30 segundos após o envio")
    void sendResponseFields() {
        Message message = messageFrom(prePaidClient(1L, "9.50"));

        SendMessageResponse response = mapper.toSendResponse(message);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.conversationId()).isEqualTo(7L);
        assertThat(response.status()).isEqualTo(MessageStatus.QUEUED);
        assertThat(response.cost()).isEqualByComparingTo("0.50");
        assertThat(response.estimatedDelivery()).isEqualTo(message.getCreatedAt().plusSeconds(30));
    }

    @Test
    @DisplayName("toResponse leva os ids da conversa e do remetente")
    void toResponse() {
        Message message = messageFrom(prePaidClient(1L, "9.50"));

        MessageResponse response = mapper.toResponse(message);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.conversationId()).isEqualTo(7L);
        assertThat(response.senderId()).isEqualTo(1L);
        assertThat(response.recipientId()).isEqualTo(55);
        assertThat(response.content()).isEqualTo("Olá");
        assertThat(response.priority()).isEqualTo(MessagePriority.URGENT);
    }

    @Test
    @DisplayName("entrada nula vira saída nula")
    void nullInput() {
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toSendResponse(null)).isNull();
    }
}
