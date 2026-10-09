package com.carloslonghi.bcb.integration;

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
        Long clientId = createPrePaidClient("12345678901", "10.00");
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
}
