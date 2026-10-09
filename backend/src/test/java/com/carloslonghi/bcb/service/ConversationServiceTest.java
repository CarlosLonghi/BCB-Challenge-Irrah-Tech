package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.controller.request.MessageRequest;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.entity.Conversation;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.exception.MissingRecipientException;
import com.carloslonghi.bcb.exception.ReferencedEntityNotFoundException;
import com.carloslonghi.bcb.repository.ConversationRepository;
import com.carloslonghi.bcb.repository.MessageRepository;
import com.carloslonghi.bcb.support.AuthenticatedClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.carloslonghi.bcb.support.TestData.conversation;
import static com.carloslonghi.bcb.support.TestData.message;
import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ConversationService conversationService;

    private Client client;

    @BeforeEach
    void setUp() {
        AuthenticatedClient.login(1L);
        client = prePaidClient(1L, "10.00");
    }

    @AfterEach
    void tearDown() {
        AuthenticatedClient.logout();
    }

    @Test
    @DisplayName("findAll lista as conversas do cliente autenticado")
    void findAll() {
        Conversation conversation = conversation(7L, client);
        when(conversationRepository.findByClientIdOrderByLastMessageTimeDesc(1L)).thenReturn(List.of(conversation));

        assertThat(conversationService.findAll()).containsExactly(conversation);
    }

    @Test
    @DisplayName("findById devolve conversa do próprio cliente")
    void findOwnConversation() {
        Conversation conversation = conversation(7L, client);
        when(conversationRepository.findById(7L)).thenReturn(Optional.of(conversation));

        assertThat(conversationService.findById(7L)).isSameAs(conversation);
    }

    @Test
    @DisplayName("conversa de outro cliente conta como inexistente")
    void otherClientConversationNotFound() {
        Conversation conversation = conversation(7L, prePaidClient(2L, "1.00"));
        when(conversationRepository.findById(7L)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.findById(7L))
                .isInstanceOf(ReferencedEntityNotFoundException.class);
    }

    @Test
    @DisplayName("findById de conversa inexistente lança not found")
    void missingConversation() {
        when(conversationRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.findById(7L))
                .isInstanceOf(ReferencedEntityNotFoundException.class);
    }

    @Test
    @DisplayName("findOrCreate com conversationId usa a conversa existente")
    void usesExistingConversation() {
        Conversation conversation = conversation(7L, client);
        when(conversationRepository.findById(7L)).thenReturn(Optional.of(conversation));

        MessageRequest request = new MessageRequest(7L, null, null, "Oi", MessagePriority.NORMAL);

        assertThat(conversationService.findOrCreate(client, request)).isSameAs(conversation);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("findOrCreate sem conversationId cria a conversa com o destinatário")
    void createsConversation() {
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageRequest request = new MessageRequest(null, 55, "Maria Souza", "Oi", MessagePriority.NORMAL);
        Conversation created = conversationService.findOrCreate(client, request);

        assertThat(created.getClient()).isSameAs(client);
        assertThat(created.getRecipientId()).isEqualTo(55);
        assertThat(created.getRecipientName()).isEqualTo("Maria Souza");
        assertThat(created.getLastMessageContent()).isEqualTo("Oi");
        assertThat(created.getLastMessageTime()).isNotNull();
        assertThat(created.getUnreadCount()).isZero();
    }

    static Stream<Arguments> missingRecipient() {
        return Stream.of(
                Arguments.of(null, "Maria Souza"),
                Arguments.of(55, null),
                Arguments.of(55, "   ")
        );
    }

    @ParameterizedTest(name = "recipientId={0}, recipientName=''{1}''")
    @MethodSource("missingRecipient")
    @DisplayName("findOrCreate sem conversationId exige destinatário completo")
    void requiresRecipient(Integer recipientId, String recipientName) {
        MessageRequest request = new MessageRequest(null, recipientId, recipientName, "Oi", MessagePriority.NORMAL);

        assertThatThrownBy(() -> conversationService.findOrCreate(client, request))
                .isInstanceOf(MissingRecipientException.class);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("findMessages lista as mensagens da conversa do cliente")
    void findMessages() {
        Conversation conversation = conversation(7L, client);
        Message message = message(1L, conversation, MessagePriority.NORMAL, MessageStatus.SENT);
        when(conversationRepository.findById(7L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(7L)).thenReturn(List.of(message));

        assertThat(conversationService.findMessages(7L)).containsExactly(message);
    }
}
