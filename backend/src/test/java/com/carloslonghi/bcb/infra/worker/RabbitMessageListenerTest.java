package com.carloslonghi.bcb.infra.worker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMessageListenerTest {

    @Mock
    private MessageProcessor messageProcessor;

    @InjectMocks
    private RabbitMessageListener listener;

    @Test
    @DisplayName("converte o id recebido da fila e repassa para o processor")
    void delegatesToProcessor() throws InterruptedException {
        listener.onMessage("42");

        verify(messageProcessor).process(42L);
    }
}
