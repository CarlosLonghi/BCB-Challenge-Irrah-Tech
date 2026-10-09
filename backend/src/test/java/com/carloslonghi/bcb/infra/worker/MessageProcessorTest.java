package com.carloslonghi.bcb.infra.worker;

import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.carloslonghi.bcb.support.TestData.conversation;
import static com.carloslonghi.bcb.support.TestData.message;
import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MessageProcessor messageProcessor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(messageProcessor, "stepDelayMs", 0L);
    }

    private Message messageWithStatus(MessageStatus status) {
        return message(1L, conversation(7L, prePaidClient(1L, "1.00")), MessagePriority.NORMAL, status);
    }

    @ParameterizedTest
    @EnumSource(value = MessageStatus.class, names = {"QUEUED", "PROCESSING", "SENT"})
    @DisplayName("mensagem pendente passa por PROCESSING, SENT e DELIVERED, nessa ordem")
    void processesPendingMessage(MessageStatus status) throws InterruptedException {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(messageWithStatus(status)));

        messageProcessor.process(1L);

        InOrder inOrder = inOrder(messageRepository);
        inOrder.verify(messageRepository).updateStatus(1L, MessageStatus.PROCESSING);
        inOrder.verify(messageRepository).updateStatus(1L, MessageStatus.SENT);
        inOrder.verify(messageRepository).updateStatus(1L, MessageStatus.DELIVERED);
    }

    @ParameterizedTest
    @EnumSource(value = MessageStatus.class, names = {"DELIVERED", "READ", "FAILED"})
    @DisplayName("mensagem já finalizada (ex.: entregue duas vezes pela fila) não é reprocessada")
    void skipsFinishedMessage(MessageStatus status) throws InterruptedException {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(messageWithStatus(status)));

        messageProcessor.process(1L);

        verify(messageRepository, never()).updateStatus(anyLong(), any());
    }

    @Test
    @DisplayName("mensagem apagada não é processada")
    void skipsMissingMessage() throws InterruptedException {
        when(messageRepository.findById(1L)).thenReturn(Optional.empty());

        messageProcessor.process(1L);

        verify(messageRepository, never()).updateStatus(anyLong(), any());
    }

    @Test
    @DisplayName("markFailed grava FAILED")
    void markFailed() {
        messageProcessor.markFailed(1L);

        verify(messageRepository).updateStatus(1L, MessageStatus.FAILED);
    }
}
