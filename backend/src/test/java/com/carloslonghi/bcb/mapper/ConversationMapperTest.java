package com.carloslonghi.bcb.mapper;

import com.carloslonghi.bcb.controller.response.ConversationResponse;
import com.carloslonghi.bcb.entity.Conversation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static com.carloslonghi.bcb.support.TestData.conversation;
import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.assertj.core.api.Assertions.assertThat;

class ConversationMapperTest {

    private final ConversationMapper mapper = Mappers.getMapper(ConversationMapper.class);

    @Test
    @DisplayName("toResponse leva o id do cliente dono da conversa")
    void toResponse() {
        Conversation conversation = conversation(7L, prePaidClient(1L, "10.00"));

        ConversationResponse response = mapper.toResponse(conversation);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.clientId()).isEqualTo(1L);
        assertThat(response.recipientId()).isEqualTo(55);
        assertThat(response.recipientName()).isEqualTo("Maria Souza");
        assertThat(response.unreadCount()).isZero();
    }

    @Test
    @DisplayName("entrada nula vira saída nula")
    void nullInput() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    @DisplayName("conversa sem cliente carregado vira clientId nulo")
    void missingClient() {
        Conversation conversation = conversation(7L, prePaidClient(1L, "10.00"));
        conversation.setClient(null);

        assertThat(mapper.toResponse(conversation).clientId()).isNull();
    }
}
