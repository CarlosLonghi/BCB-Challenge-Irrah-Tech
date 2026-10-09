package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.controller.request.MessageRequest;
import com.carloslonghi.bcb.controller.response.MessageResponse;
import com.carloslonghi.bcb.controller.response.SendMessageResponse;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.exception.InsufficientBalanceException;
import com.carloslonghi.bcb.exception.ReferencedEntityNotFoundException;
import com.carloslonghi.bcb.mapper.MessageMapper;
import com.carloslonghi.bcb.service.MessageService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock
    private MessageService messageService;
    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private MessageController controller;

    private final Message message =
            message(1L, conversation(7L, prePaidClient(1L, "10.00")), MessagePriority.NORMAL, MessageStatus.QUEUED);

    @Test
    @DisplayName("send devolve 201 com o resumo do envio")
    void send() {
        MessageRequest request = new MessageRequest(7L, null, null, "Olá", MessagePriority.NORMAL);
        SendMessageResponse sendResponse = mock(SendMessageResponse.class);
        when(messageService.sendMessage(request)).thenReturn(message);
        when(messageMapper.toSendResponse(message)).thenReturn(sendResponse);

        ResponseEntity<SendMessageResponse> response = controller.send(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(sendResponse);
    }

    @Test
    @DisplayName("findAll e findById devolvem as mensagens mapeadas")
    void findAllAndById() {
        MessageResponse messageResponse = mock(MessageResponse.class);
        when(messageService.findAll()).thenReturn(List.of(message));
        when(messageService.findById(1L)).thenReturn(message);
        when(messageMapper.toResponse(message)).thenReturn(messageResponse);

        assertThat(controller.findAll().getBody()).containsExactly(messageResponse);
        assertThat(controller.findById(1L).getBody()).isSameAs(messageResponse);
    }

    @Test
    @DisplayName("getStatus devolve só o status da mensagem")
    void getStatus() {
        when(messageService.getStatus(1L)).thenReturn(MessageStatus.SENT);

        ResponseEntity<MessageStatus> response = controller.getStatus(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(MessageStatus.SENT);
    }

    @Test
    @DisplayName("send sem saldo propaga o erro sem montar resposta")
    void sendWithoutBalance() {
        MessageRequest request = new MessageRequest(7L, null, null, "Olá", MessagePriority.URGENT);
        when(messageService.sendMessage(request)).thenThrow(new InsufficientBalanceException());

        assertThatThrownBy(() -> controller.send(request))
                .isInstanceOf(InsufficientBalanceException.class);
        verifyNoInteractions(messageMapper);
    }

    @Test
    @DisplayName("status de mensagem inexistente propaga not found")
    void statusOfMissingMessage() {
        when(messageService.getStatus(9L)).thenThrow(new ReferencedEntityNotFoundException("Mensagem", 9L));

        assertThatThrownBy(() -> controller.getStatus(9L))
                .isInstanceOf(ReferencedEntityNotFoundException.class);
    }
}
