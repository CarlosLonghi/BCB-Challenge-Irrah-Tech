package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.controller.response.ConversationResponse;
import com.carloslonghi.bcb.controller.response.MessageResponse;
import com.carloslonghi.bcb.entity.Conversation;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.mapper.ConversationMapper;
import com.carloslonghi.bcb.mapper.MessageMapper;
import com.carloslonghi.bcb.service.ConversationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static com.carloslonghi.bcb.support.TestData.conversation;
import static com.carloslonghi.bcb.support.TestData.message;
import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationControllerTest {

    @Mock
    private ConversationService conversationService;
    @Mock
    private ConversationMapper conversationMapper;
    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private ConversationController controller;

    private final Conversation conversation = conversation(7L, prePaidClient(1L, "10.00"));
    private final ConversationResponse conversationResponse = mock(ConversationResponse.class);

    @Test
    @DisplayName("findAll devolve as conversas mapeadas")
    void findAll() {
        when(conversationService.findAll()).thenReturn(List.of(conversation));
        when(conversationMapper.toResponse(conversation)).thenReturn(conversationResponse);

        ResponseEntity<List<ConversationResponse>> response = controller.findAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(conversationResponse);
    }

    @Test
    @DisplayName("findById devolve a conversa")
    void findById() {
        when(conversationService.findById(7L)).thenReturn(conversation);
        when(conversationMapper.toResponse(conversation)).thenReturn(conversationResponse);

        ResponseEntity<ConversationResponse> response = controller.findById(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(conversationResponse);
    }

    @Test
    @DisplayName("findMessagesByConversationId devolve as mensagens da conversa")
    void findMessages() {
        Message message = message(1L, conversation, MessagePriority.NORMAL, MessageStatus.SENT);
        MessageResponse messageResponse = mock(MessageResponse.class);
        when(conversationService.findMessages(7L)).thenReturn(List.of(message));
        when(messageMapper.toResponse(message)).thenReturn(messageResponse);

        ResponseEntity<List<MessageResponse>> response = controller.findMessagesByConversationId(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(messageResponse);
    }
}
