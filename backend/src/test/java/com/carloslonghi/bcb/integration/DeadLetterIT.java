package com.carloslonghi.bcb.integration;

import com.carloslonghi.bcb.service.MessageService;
import com.carloslonghi.bcb.config.RabbitConfig;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Retry do listener + MessageRecoverer: esgotadas as tentativas, a mensagem vira FAILED e vai para a DLQ
class DeadLetterIT extends IntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private MessageService messageService;

    @BeforeEach
    void purgeDeadLetterQueue() {
        amqpAdmin.purgeQueue(RabbitConfig.DEAD_LETTER_QUEUE, false);
    }

    @Test
    @DisplayName("id inválido na fila vai para a dead-letter queue depois das tentativas")
    void invalidIdGoesToDeadLetter() {
        rabbitTemplate.convertAndSend(RabbitConfig.MESSAGES_QUEUE, "abc");

        Object deadLetter = rabbitTemplate.receiveAndConvert(RabbitConfig.DEAD_LETTER_QUEUE, 10_000);
        assertThat(deadLetter).isEqualTo("abc");
    }

    @Test
    @DisplayName("falha em todas as tentativas marca FAILED, estorna o custo e manda para a DLQ")
    void processingFailureMarksFailedAndRefunds() throws Exception {
        Long clientId = createPrePaidClient("12345678909", "10.00");
        String token = login("12345678909");
        // A mensagem enviada a seguir tera id 1 (TRUNCATE ... RESTART IDENTITY)
        doThrow(new DataAccessResourceFailureException("falha no worker"))
                .when(messageRepository).updateStatus(eq(1L), eq(MessageStatus.PROCESSING));

        String sent = mockMvc.perform(withToken(post("/messages"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson(null, 55, "Maria Souza", "NORMAL")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.parse(sent).read("$.id", Long.class)).isEqualTo(1L);

        await().atMost(Duration.ofSeconds(15)).until(() -> "FAILED".equals(jdbcTemplate.queryForObject(
                "SELECT status FROM tb_messages WHERE id = 1", String.class)));
        assertThat(rabbitTemplate.receiveAndConvert(RabbitConfig.DEAD_LETTER_QUEUE, 10_000)).isEqualTo("1");

        assertThat(balanceOf(clientId)).isEqualByComparingTo("10.00");
        assertThat(jdbcTemplate.queryForList("SELECT type FROM tb_ledger_entries WHERE message_id = 1 ORDER BY id", String.class))
                .containsExactly("DEBIT", "REFUND");
    }

    @Test
    @DisplayName("mensagem que passa de novo pelo recoverer não é estornada duas vezes")
    void secondFailureDoesNotRefundAgain() throws Exception {
        Long clientId = createPostPaidClient("12345678909", "5.00");
        String token = login("12345678909");
        doThrow(new DataAccessResourceFailureException("falha no worker"))
                .when(messageRepository).updateStatus(eq(1L), eq(MessageStatus.PROCESSING));
        mockMvc.perform(withToken(post("/messages"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson(null, 55, "Maria Souza", "URGENT")))
                .andExpect(status().isCreated());
        await().atMost(Duration.ofSeconds(15)).until(() -> "FAILED".equals(jdbcTemplate.queryForObject(
                "SELECT status FROM tb_messages WHERE id = 1", String.class)));
        assertThat(rabbitTemplate.receiveAndConvert(RabbitConfig.DEAD_LETTER_QUEUE, 10_000)).isEqualTo("1");

        // O recoverer roda de novo para a mesma mensagem (ex.: duas entregas do mesmo id esgotando as tentativas)
        messageService.failAndRefund(1L);

        assertThat(limitOf(clientId)).isEqualByComparingTo("5.00");
        assertThat(count("tb_ledger_entries")).isEqualTo(2);
    }
}
