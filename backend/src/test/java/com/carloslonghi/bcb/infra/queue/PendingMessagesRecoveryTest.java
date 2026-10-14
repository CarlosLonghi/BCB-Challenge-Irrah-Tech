package com.carloslonghi.bcb.infra.queue;

import com.carloslonghi.bcb.entity.Conversation;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.ConnectException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static com.carloslonghi.bcb.support.TestData.conversation;
import static com.carloslonghi.bcb.support.TestData.message;
import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingMessagesRecoveryTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private MessageQueue messageQueue;

    private PendingMessagesRecovery recovery;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        recovery = new PendingMessagesRecovery(messageRepository, messageQueue, Clock.fixed(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(recovery, "unpublishedGrace", Duration.ofSeconds(10));
        conversation = conversation(7L, prePaidClient(1L, "1.00"));
    }

    @Nested
    @DisplayName("na inicialização")
    class OnStartup {

        @Test
        @DisplayName("busca QUEUED, PROCESSING e SENT, volta cada uma para QUEUED sem publicação e reenfileira na ordem")
        void requeuesPendingMessages() {
            Message first = message(1L, conversation, MessagePriority.NORMAL, MessageStatus.PROCESSING);
            Message second = message(2L, conversation, MessagePriority.URGENT, MessageStatus.SENT);
            when(messageRepository.findByStatusInOrderByCreatedAtAsc(
                    List.of(MessageStatus.QUEUED, MessageStatus.PROCESSING, MessageStatus.SENT)))
                    .thenReturn(List.of(first, second));

            recovery.requeuePendingMessages();

            InOrder inOrder = inOrder(messageRepository, messageQueue);
            inOrder.verify(messageRepository).resetToUnpublished(1L);
            inOrder.verify(messageQueue).enqueue(first);
            inOrder.verify(messageRepository).markEnqueued(1L, NOW);
            inOrder.verify(messageRepository).resetToUnpublished(2L);
            inOrder.verify(messageQueue).enqueue(second);
            inOrder.verify(messageRepository).markEnqueued(2L, NOW);
        }

        @Test
        @DisplayName("sem pendentes, não enfileira nada")
        void nothingPending() {
            when(messageRepository.findByStatusInOrderByCreatedAtAsc(anyList())).thenReturn(List.of());

            recovery.requeuePendingMessages();

            verify(messageRepository).findByStatusInOrderByCreatedAtAsc(anyList());
            verifyNoInteractions(messageQueue);
        }

        @Test
        @DisplayName("RabbitMQ fora do ar: não derruba a inicialização, não marca como publicada e tenta as demais")
        void brokerDownDoesNotStopRecovery() {
            Message first = message(1L, conversation, MessagePriority.NORMAL, MessageStatus.QUEUED);
            Message second = message(2L, conversation, MessagePriority.URGENT, MessageStatus.PROCESSING);
            when(messageRepository.findByStatusInOrderByCreatedAtAsc(anyList())).thenReturn(List.of(first, second));
            doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                    .when(messageQueue).enqueue(first);

            assertThatCode(() -> recovery.requeuePendingMessages()).doesNotThrowAnyException();

            verify(messageRepository).resetToUnpublished(1L);
            verify(messageRepository, never()).markEnqueued(1L, NOW);
            verify(messageQueue).enqueue(second);
            verify(messageRepository).markEnqueued(2L, NOW);
        }

        @Test
        @DisplayName("falha do banco numa mensagem não a enfileira, mas segue para as demais")
        void databaseFailureSkipsOnlyThatMessage() {
            Message first = message(1L, conversation, MessagePriority.NORMAL, MessageStatus.SENT);
            Message second = message(2L, conversation, MessagePriority.NORMAL, MessageStatus.QUEUED);
            when(messageRepository.findByStatusInOrderByCreatedAtAsc(anyList())).thenReturn(List.of(first, second));
            doThrow(new DataAccessResourceFailureException("banco fora do ar"))
                    .when(messageRepository).resetToUnpublished(1L);

            assertThatCode(() -> recovery.requeuePendingMessages()).doesNotThrowAnyException();

            verify(messageQueue, never()).enqueue(first);
            verify(messageQueue).enqueue(second);
        }
    }

    @Nested
    @DisplayName("rotina periódica")
    class Periodic {

        @Test
        @DisplayName("publica as QUEUED sem publicação criadas antes da folga e marca a publicação")
        void republishesUnpublished() {
            Message stuck = message(1L, conversation, MessagePriority.URGENT, MessageStatus.QUEUED);
            when(messageRepository.findByStatusAndEnqueuedAtIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(
                    MessageStatus.QUEUED, NOW.minusSeconds(10))).thenReturn(List.of(stuck));

            recovery.requeueUnpublished();

            InOrder inOrder = inOrder(messageQueue, messageRepository);
            inOrder.verify(messageQueue).enqueue(stuck);
            inOrder.verify(messageRepository).markEnqueued(1L, NOW);
            // Nao mexe no status: a mensagem ja esta QUEUED
            verify(messageRepository, never()).resetToUnpublished(anyLong());
        }

        @Test
        @DisplayName("sem mensagens perdidas, não publica nada")
        void nothingUnpublished() {
            when(messageRepository.findByStatusAndEnqueuedAtIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(any(), any()))
                    .thenReturn(List.of());

            recovery.requeueUnpublished();

            verifyNoInteractions(messageQueue);
        }

        @Test
        @DisplayName("RabbitMQ ainda fora do ar: segue sem publicação para a próxima rodada e tenta as demais")
        void brokerStillDown() {
            Message first = message(1L, conversation, MessagePriority.NORMAL, MessageStatus.QUEUED);
            Message second = message(2L, conversation, MessagePriority.NORMAL, MessageStatus.QUEUED);
            when(messageRepository.findByStatusAndEnqueuedAtIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(any(), any()))
                    .thenReturn(List.of(first, second));
            doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                    .when(messageQueue).enqueue(first);

            assertThatCode(() -> recovery.requeueUnpublished()).doesNotThrowAnyException();

            verify(messageRepository, never()).markEnqueued(1L, NOW);
            verify(messageQueue).enqueue(second);
            verify(messageRepository).markEnqueued(2L, NOW);
        }
    }
}
