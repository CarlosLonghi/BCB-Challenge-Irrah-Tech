package com.carloslonghi.bcb.integration;

import com.carloslonghi.bcb.controller.request.MessageRequest;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.service.MessageService;
import com.carloslonghi.bcb.support.AuthenticatedClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// Prova o @Transactional de MessageService.sendMessage: debito, mensagem e conversa sao gravados juntos
class MessageTransactionIT extends IntegrationTest {

    @Autowired
    private MessageService messageService;

    @AfterEach
    void logout() {
        AuthenticatedClient.logout();
    }

    @Test
    @DisplayName("falha ao salvar a mensagem depois do débito desfaz o débito e a conversa nova")
    void failureAfterDebitRollsBack() throws Exception {
        Long clientId = createPrePaidClient("12345678901", "10.00");
        AuthenticatedClient.login(clientId);
        doThrow(new DataAccessResourceFailureException("banco caiu no meio do envio"))
                .when(messageRepository).save(any(Message.class));

        MessageRequest request = new MessageRequest(null, 55, "Maria Souza", "Olá", MessagePriority.URGENT);
        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(DataAccessResourceFailureException.class);

        assertThat(balanceOf(clientId)).isEqualByComparingTo("10.00");
        assertThat(count("tb_conversations")).isZero();
        assertThat(count("tb_messages")).isZero();
        verify(messageQueue, never()).enqueue(any());
    }

    @Test
    @DisplayName("envio com sucesso grava débito, mensagem e conversa, e só então enfileira")
    void successCommitsEverything() throws Exception {
        Long clientId = createPrePaidClient("12345678901", "10.00");
        AuthenticatedClient.login(clientId);

        Message message = messageService.sendMessage(
                new MessageRequest(null, 55, "Maria Souza", "Olá", MessagePriority.URGENT));

        assertThat(balanceOf(clientId)).isEqualByComparingTo("9.50");
        assertThat(count("tb_conversations")).isEqualTo(1);
        assertThat(count("tb_messages")).isEqualTo(1);
        verify(messageQueue).enqueue(message);
        // O worker real consome a mensagem; espera terminar para nao vazar para o proximo teste
        await().until(() -> "DELIVERED".equals(jdbcTemplate.queryForObject(
                "SELECT status FROM tb_messages WHERE id = ?", String.class, message.getId())));
    }
}
