package com.carloslonghi.bcb.infra.worker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

    @Test
    @DisplayName("id inválido na fila lança exceção sem chamar o processor (vai para a dead-letter após os retries)")
    void invalidId() {
        assertThatThrownBy(() -> listener.onMessage("abc"))
                .isInstanceOf(NumberFormatException.class);
        verifyNoInteractions(messageProcessor);
    }

    @Test
    @DisplayName("erro do processor propaga para o Spring AMQP fazer o retry")
    void processorFailurePropagates() throws InterruptedException {
        doThrow(new IllegalStateException("falha")).when(messageProcessor).process(42L);

        assertThatThrownBy(() -> listener.onMessage("42"))
                .isInstanceOf(IllegalStateException.class);
    }
}
