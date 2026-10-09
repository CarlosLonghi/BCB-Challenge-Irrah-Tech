package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.controller.request.MessageRequest;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.entity.Conversation;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.exception.CreditLimitExceededException;
import com.carloslonghi.bcb.exception.InsufficientBalanceException;
import com.carloslonghi.bcb.exception.MissingRecipientException;
import com.carloslonghi.bcb.exception.ReferencedEntityNotFoundException;
import com.carloslonghi.bcb.infra.queue.MessageQueue;
import com.carloslonghi.bcb.repository.ClientRepository;
import com.carloslonghi.bcb.repository.ConversationRepository;
import com.carloslonghi.bcb.repository.MessageRepository;
import com.carloslonghi.bcb.support.AuthenticatedClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.carloslonghi.bcb.support.TestData.conversation;
import static com.carloslonghi.bcb.support.TestData.message;
import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    private static final Long SENDER_ID = 1L;

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ClientService clientService;
    @Mock
    private ConversationService conversationService;
    @Mock
    private MessageQueue messageQueue;

    @InjectMocks
    private MessageService messageService;

    private Client sender;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        AuthenticatedClient.login(SENDER_ID);
        // Fora de uma transacao real: o registro do afterCommit fica guardado para o teste disparar
        TransactionSynchronizationManager.initSynchronization();
        sender = prePaidClient(SENDER_ID, "10.00");
        conversation = conversation(7L, sender);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
        AuthenticatedClient.logout();
    }

    private MessageRequest request(MessagePriority priority) {
        return new MessageRequest(7L, null, null, "Olá, tudo bem?", priority);
    }

    private void givenSenderAndConversation(MessageRequest request) {
        when(clientRepository.findWithLockById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(conversationService.findOrCreate(sender, request)).thenReturn(conversation);
    }

    private void givenSaveReturnsWithId() {
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });
    }

    private static void commit() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    @ParameterizedTest(name = "{0} custa R$ {1}")
    @CsvSource({"NORMAL, 0.25", "URGENT, 0.50"})
    @DisplayName("sendMessage debita o custo da prioridade do remetente")
    void debitsCostByPriority(MessagePriority priority, BigDecimal expectedCost) {
        MessageRequest request = request(priority);
        givenSenderAndConversation(request);
        givenSaveReturnsWithId();

        Message message = messageService.sendMessage(request);

        verify(clientService).debit(eq(sender), argThat(cost -> cost.compareTo(expectedCost) == 0));
        assertThat(message.getCost()).isEqualByComparingTo(expectedCost);
    }

    @Test
    @DisplayName("sendMessage busca o remetente com lock, para envios simultâneos não lerem o mesmo saldo")
    void loadsSenderWithLock() {
        MessageRequest request = request(MessagePriority.NORMAL);
        givenSenderAndConversation(request);
        givenSaveReturnsWithId();

        messageService.sendMessage(request);

        verify(clientRepository).findWithLockById(SENDER_ID);
        verify(clientRepository, never()).findById(any());
    }

    @Test
    @DisplayName("sendMessage salva a mensagem QUEUED com os dados do request e da conversa")
    void savesQueuedMessage() {
        MessageRequest request = request(MessagePriority.URGENT);
        givenSenderAndConversation(request);
        givenSaveReturnsWithId();

        Message message = messageService.sendMessage(request);

        assertThat(message.getId()).isEqualTo(99L);
        assertThat(message.getStatus()).isEqualTo(MessageStatus.QUEUED);
        assertThat(message.getContent()).isEqualTo("Olá, tudo bem?");
        assertThat(message.getPriority()).isEqualTo(MessagePriority.URGENT);
        assertThat(message.getSender()).isSameAs(sender);
        assertThat(message.getConversation()).isSameAs(conversation);
        assertThat(message.getRecipientId()).isEqualTo(conversation.getRecipientId());
        assertThat(message.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("sendMessage atualiza a última mensagem da conversa")
    void updatesConversation() {
        MessageRequest request = request(MessagePriority.NORMAL);
        givenSenderAndConversation(request);
        givenSaveReturnsWithId();

        Message message = messageService.sendMessage(request);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertThat(captor.getValue().getLastMessageContent()).isEqualTo("Olá, tudo bem?");
        assertThat(captor.getValue().getLastMessageTime()).isEqualTo(message.getCreatedAt());
    }

    @Test
    @DisplayName("sendMessage só enfileira depois do commit")
    void enqueuesOnlyAfterCommit() {
        MessageRequest request = request(MessagePriority.NORMAL);
        givenSenderAndConversation(request);
        givenSaveReturnsWithId();

        Message message = messageService.sendMessage(request);

        verifyNoInteractions(messageQueue);
        commit();
        verify(messageQueue).enqueue(message);
    }

    @Test
    @DisplayName("falha da fila depois do commit não propaga: a mensagem fica QUEUED para a recuperação")
    void queueFailureIsSwallowed() {
        MessageRequest request = request(MessagePriority.NORMAL);
        givenSenderAndConversation(request);
        givenSaveReturnsWithId();
        doThrow(new RuntimeException("RabbitMQ fora do ar")).when(messageQueue).enqueue(any());

        Message message = messageService.sendMessage(request);

        assertThatCode(MessageServiceTest::commit).doesNotThrowAnyException();
        assertThat(message.getStatus()).isEqualTo(MessageStatus.QUEUED);
    }

    @Test
    @DisplayName("sem saldo, a exceção propaga e nada é salvo nem enfileirado")
    void insufficientBalanceSavesNothing() {
        MessageRequest request = request(MessagePriority.URGENT);
        givenSenderAndConversation(request);
        doThrow(new InsufficientBalanceException()).when(clientService).debit(eq(sender), any());

        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(messageRepository, never()).save(any());
        verify(conversationRepository, never()).save(any());
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
        verifyNoInteractions(messageQueue);
    }

    @Test
    @DisplayName("remetente inexistente lança not found sem cobrar")
    void missingSender() {
        when(clientRepository.findWithLockById(SENDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.sendMessage(request(MessagePriority.NORMAL)))
                .isInstanceOf(ReferencedEntityNotFoundException.class);
        verifyNoInteractions(clientService, messageRepository, messageQueue);
    }

    @Test
    @DisplayName("findAll devolve as mensagens enviadas pelo cliente autenticado")
    void findAll() {
        Message message = message(1L, conversation, MessagePriority.NORMAL, MessageStatus.SENT);
        when(messageRepository.findBySenderId(SENDER_ID)).thenReturn(List.of(message));

        assertThat(messageService.findAll()).containsExactly(message);
    }

    @Test
    @DisplayName("findById e getStatus devolvem a mensagem do próprio remetente")
    void findOwnMessage() {
        Message message = message(1L, conversation, MessagePriority.NORMAL, MessageStatus.DELIVERED);
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));

        assertThat(messageService.findById(1L)).isSameAs(message);
        assertThat(messageService.getStatus(1L)).isEqualTo(MessageStatus.DELIVERED);
    }

    @Test
    @DisplayName("mensagem de outro remetente conta como inexistente")
    void otherSenderMessageNotFound() {
        Conversation otherConversation = conversation(8L, prePaidClient(2L, "1.00"));
        Message message = message(1L, otherConversation, MessagePriority.NORMAL, MessageStatus.SENT);
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> messageService.findById(1L))
                .isInstanceOf(ReferencedEntityNotFoundException.class);
    }

    @Test
    @DisplayName("pós-pago sem limite: a exceção propaga e nada é salvo nem enfileirado")
    void creditLimitExceededSavesNothing() {
        MessageRequest request = request(MessagePriority.NORMAL);
        givenSenderAndConversation(request);
        doThrow(new CreditLimitExceededException()).when(clientService).debit(eq(sender), any());

        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(CreditLimitExceededException.class);

        verify(messageRepository, never()).save(any());
        verify(conversationRepository, never()).save(any());
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    @Test
    @DisplayName("conversa nova sem destinatário: falha antes de cobrar")
    void missingRecipientDoesNotCharge() {
        MessageRequest request = new MessageRequest(null, null, null, "Oi", MessagePriority.NORMAL);
        when(clientRepository.findWithLockById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(conversationService.findOrCreate(sender, request)).thenThrow(new MissingRecipientException());

        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(MissingRecipientException.class);

        verifyNoInteractions(clientService, messageRepository, messageQueue);
    }

    @Test
    @DisplayName("conversa de outro cliente ou inexistente: falha antes de cobrar")
    void unknownConversationDoesNotCharge() {
        MessageRequest request = request(MessagePriority.URGENT);
        when(clientRepository.findWithLockById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(conversationService.findOrCreate(sender, request))
                .thenThrow(new ReferencedEntityNotFoundException("Conversa", 7L));

        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(ReferencedEntityNotFoundException.class);

        verifyNoInteractions(clientService, messageRepository, messageQueue);
    }

    @Test
    @DisplayName("falha ao salvar a mensagem propaga e não registra o enfileiramento")
    void messageSaveFailure() {
        MessageRequest request = request(MessagePriority.NORMAL);
        givenSenderAndConversation(request);
        when(messageRepository.save(any(Message.class))).thenThrow(new DataAccessResourceFailureException("banco fora do ar"));

        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(DataAccessResourceFailureException.class);

        verify(conversationRepository, never()).save(any());
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
        verifyNoInteractions(messageQueue);
    }

    @Test
    @DisplayName("se a transação fizer rollback, a mensagem não é enfileirada")
    void rollbackDoesNotEnqueue() {
        MessageRequest request = request(MessagePriority.NORMAL);
        givenSenderAndConversation(request);
        givenSaveReturnsWithId();

        messageService.sendMessage(request);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(messageQueue);
    }

    @Test
    @DisplayName("findById e getStatus de mensagem inexistente lançam not found")
    void missingMessage() {
        when(messageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.findById(1L))
                .isInstanceOf(ReferencedEntityNotFoundException.class);
        assertThatThrownBy(() -> messageService.getStatus(1L))
                .isInstanceOf(ReferencedEntityNotFoundException.class)
                .hasMessage("Mensagem de id 1 não encontrado(a).");
    }
}
