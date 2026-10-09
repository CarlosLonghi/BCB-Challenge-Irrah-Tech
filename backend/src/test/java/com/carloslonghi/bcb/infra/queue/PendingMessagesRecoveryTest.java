package com.carloslonghi.bcb.infra.queue;

import com.carloslonghi.bcb.entity.Conversation;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.repository.MessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.carloslonghi.bcb.support.TestData.conversation;
import static com.carloslonghi.bcb.support.TestData.message;
import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingMessagesRecoveryTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private MessageQueue messageQueue;

    @InjectMocks
    private PendingMessagesRecovery recovery;

    @Test
    @DisplayName("busca QUEUED, PROCESSING e SENT, volta cada uma para QUEUED e reenfileira na ordem")
    void requeuesPendingMessages() {
        Conversation conversation = conversation(7L, prePaidClient(1L, "1.00"));
        Message first = message(1L, conversation, MessagePriority.NORMAL, MessageStatus.PROCESSING);
        Message second = message(2L, conversation, MessagePriority.URGENT, MessageStatus.SENT);
        when(messageRepository.findByStatusInOrderByCreatedAtAsc(
                List.of(MessageStatus.QUEUED, MessageStatus.PROCESSING, MessageStatus.SENT)))
                .thenReturn(List.of(first, second));

        recovery.requeuePendingMessages();

        InOrder inOrder = inOrder(messageRepository, messageQueue);
        inOrder.verify(messageRepository).updateStatus(1L, MessageStatus.QUEUED);
        inOrder.verify(messageQueue).enqueue(first);
        inOrder.verify(messageRepository).updateStatus(2L, MessageStatus.QUEUED);
        inOrder.verify(messageQueue).enqueue(second);
    }

    @Test
    @DisplayName("sem pendentes, não enfileira nada")
    void nothingPending() {
        when(messageRepository.findByStatusInOrderByCreatedAtAsc(anyList())).thenReturn(List.of());

        recovery.requeuePendingMessages();

        verify(messageRepository).findByStatusInOrderByCreatedAtAsc(anyList());
        verifyNoInteractions(messageQueue);
    }
}
