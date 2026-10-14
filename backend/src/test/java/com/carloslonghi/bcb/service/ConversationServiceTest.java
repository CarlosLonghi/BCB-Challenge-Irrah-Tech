package com.carloslonghi.bcb.service;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Limit;
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
    @DisplayName("findAll lista a página pedida das conversas do cliente, da mais recente para a mais antiga")
    void findAll() {
        Conversation conversation = conversation(7L, client);
        Pageable expected = PageRequest.of(2, 20, Sort.by(Sort.Order.desc("lastMessageTime"), Sort.Order.desc("id")));
        when(conversationRepository.findByClientId(1L, expected)).thenReturn(List.of(conversation));

        assertThat(conversationService.findAll(2, 20)).containsExactly(conversation);
    }

    @ParameterizedTest(name = "page={0}, size={1} -> page={2}, size={3}")
    @CsvSource({"-1, 50, 0, 50", "0, 0, 0, 1", "0, 500, 0, 100"})
    @DisplayName("findAll ajusta página negativa e tamanho fora de 1..100")
    void findAllClampsPage(int page, int size, int expectedPage, int expectedSize) {
        when(conversationRepository.findByClientId(eq(1L), any(Pageable.class))).thenReturn(List.of());

        conversationService.findAll(page, size);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(conversationRepository).findByClientId(eq(1L), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(expectedPage);
        assertThat(captor.getValue().getPageSize()).isEqualTo(expectedSize);
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

        MessageRequest request = new MessageRequest(null, 55L, "Maria Souza", "Oi", MessagePriority.NORMAL);
        Conversation created = conversationService.findOrCreate(client, request);

        assertThat(created.getClient()).isSameAs(client);
        assertThat(created.getRecipientId()).isEqualTo(55L);
        assertThat(created.getRecipientName()).isEqualTo("Maria Souza");
        assertThat(created.getLastMessageContent()).isEqualTo("Oi");
        assertThat(created.getLastMessageTime()).isNotNull();
        assertThat(created.getUnreadCount()).isZero();
    }

    static Stream<Arguments> missingRecipient() {
        return Stream.of(
                Arguments.of(null, "Maria Souza"),
                Arguments.of(55L, null),
                Arguments.of(55L, "   ")
        );
    }

    @ParameterizedTest(name = "recipientId={0}, recipientName=''{1}''")
    @MethodSource("missingRecipient")
    @DisplayName("findOrCreate sem conversationId exige destinatário completo")
    void requiresRecipient(Long recipientId, String recipientName) {
        MessageRequest request = new MessageRequest(null, recipientId, recipientName, "Oi", MessagePriority.NORMAL);

        assertThatThrownBy(() -> conversationService.findOrCreate(client, request))
                .isInstanceOf(MissingRecipientException.class);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("findMessages sem cursor busca as mais recentes e devolve em ordem crescente")
    void findMessages() {
        Conversation conversation = conversation(7L, client);
        Message older = message(1L, conversation, MessagePriority.NORMAL, MessageStatus.SENT);
        Message newer = message(2L, conversation, MessagePriority.NORMAL, MessageStatus.QUEUED);
        when(conversationRepository.findById(7L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByIdDesc(7L, Limit.of(50))).thenReturn(List.of(newer, older));

        assertThat(conversationService.findMessages(7L, null, 50)).containsExactly(older, newer);
    }

    @Test
    @DisplayName("findMessages com cursor busca as anteriores a ele, com limite ajustado ao teto")
    void findMessagesBeforeCursor() {
        Conversation conversation = conversation(7L, client);
        Message message = message(1L, conversation, MessagePriority.NORMAL, MessageStatus.SENT);
        when(conversationRepository.findById(7L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(7L, 40L, Limit.of(100)))
                .thenReturn(List.of(message));

        assertThat(conversationService.findMessages(7L, 40L, 1000)).containsExactly(message);
    }

    @Test
    @DisplayName("findMessages de conversa de outro cliente lança not found sem buscar mensagens")
    void findMessagesOfOtherClient() {
        Conversation conversation = conversation(7L, prePaidClient(2L, "1.00"));
        when(conversationRepository.findById(7L)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.findMessages(7L, null, 50))
                .isInstanceOf(ReferencedEntityNotFoundException.class);
        verifyNoInteractions(messageRepository);
    }

    @Test
    @DisplayName("findOrCreate com conversationId de outro cliente não cria conversa nova")
    void conversationIdOfOtherClient() {
        Conversation conversation = conversation(7L, prePaidClient(2L, "1.00"));
        when(conversationRepository.findById(7L)).thenReturn(Optional.of(conversation));
        MessageRequest request = new MessageRequest(7L, 55L, "Maria Souza", "Oi", MessagePriority.NORMAL);

        assertThatThrownBy(() -> conversationService.findOrCreate(client, request))
                .isInstanceOf(ReferencedEntityNotFoundException.class)
                .hasMessage("Conversa de id 7 não encontrado(a).");
        verify(conversationRepository, never()).save(any());
    }
}
