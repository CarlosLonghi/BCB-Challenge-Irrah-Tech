package com.carloslonghi.bcb.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import java.net.ConnectException;
import org.springframework.http.MediaType;
import org.springframework.amqp.AmqpConnectException;
import com.carloslonghi.bcb.infra.queue.PendingMessagesRecovery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.awaitility.Awaitility.await;

// Simula o backend reiniciando com mensagens pela metade no banco (fora da fila)
class PendingMessagesRecoveryIT extends IntegrationTest {

    @Autowired
    private PendingMessagesRecovery recovery;

    @Test
    @DisplayName("mensagens PROCESSING e SENT esquecidas voltam para a fila e são entregues")
    void requeuesAndDelivers() throws Exception {
        Long clientId = createPrePaidClient("12345678909", "10.00");
        jdbcTemplate.update("""
                INSERT INTO tb_conversations (client_id, recipient_id, recipient_name, last_message_content, last_message_time, unread_count)
                VALUES (?, 55, 'Maria Souza', 'Oi', now(), 0)
                """, clientId);
        jdbcTemplate.update("""
                INSERT INTO tb_messages (conversation_id, sender_id, recipient_id, content, created_at, priority, status, cost)
                VALUES (1, ?, 55, 'Oi', now(), 'NORMAL', 'PROCESSING', 0.25),
                       (1, ?, 55, 'Oi', now(), 'URGENT', 'SENT', 0.50)
                """, clientId, clientId);

        recovery.requeuePendingMessages();

        await().until(() -> jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tb_messages WHERE status = 'DELIVERED'", Integer.class) == 2);
    }

    @Test
    @DisplayName("publicação que falhou no envio fica sem enqueued_at e a rotina periódica a entrega sem reiniciar")
    void republishesLostPublication() throws Exception {
        createPrePaidClient("12345678909", "10.00");
        String token = login("12345678909");
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .doCallRealMethod()
                .when(messageQueue).enqueue(any());

        mockMvc.perform(withToken(post("/messages"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson(null, 55, "Maria Souza", "NORMAL")))
                .andExpect(status().isCreated());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status || '/' || coalesce(enqueued_at::text, 'null') FROM tb_messages WHERE id = 1", String.class))
                .isEqualTo("QUEUED/null");

        recovery.requeueUnpublished();

        await().until(() -> "DELIVERED".equals(jdbcTemplate.queryForObject(
                "SELECT status FROM tb_messages WHERE id = 1", String.class)));
        assertThat(jdbcTemplate.queryForObject("SELECT enqueued_at IS NOT NULL FROM tb_messages WHERE id = 1", Boolean.class))
                .isTrue();
    }

    @Test
    @DisplayName("mensagem já publicada não é republicada pela rotina periódica")
    void skipsPublishedMessages() throws Exception {
        createPrePaidClient("12345678909", "10.00");
        String token = login("12345678909");
        mockMvc.perform(withToken(post("/messages"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson(null, 55, "Maria Souza", "NORMAL")))
                .andExpect(status().isCreated());
        await().until(() -> jdbcTemplate.queryForObject(
                "SELECT enqueued_at IS NOT NULL FROM tb_messages WHERE id = 1", Boolean.class));
        clearInvocations(messageQueue);

        recovery.requeueUnpublished();

        verify(messageQueue, never()).enqueue(any());
        await().until(() -> "DELIVERED".equals(jdbcTemplate.queryForObject(
                "SELECT status FROM tb_messages WHERE id = 1", String.class)));
    }
}
