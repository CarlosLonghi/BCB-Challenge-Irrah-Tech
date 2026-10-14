package com.carloslonghi.bcb.integration;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaginationIT extends IntegrationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private String token;
    private Long conversationId;

    @BeforeEach
    void sendFiveMessages() throws Exception {
        createPrePaidClient("12345678909", "10.00");
        token = login("12345678909");
        mockMvc.perform(withToken(post("/messages"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson(null, 55, "Maria Souza", "NORMAL")))
                .andExpect(status().isCreated());
        conversationId = 1L;
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(withToken(post("/messages"), token).contentType(MediaType.APPLICATION_JSON)
                            .content(messageJson(conversationId, null, null, "NORMAL")))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @DisplayName("chat: as mais recentes em ordem crescente e, com o cursor, as anteriores")
    void chatByCursor() throws Exception {
        mockMvc.perform(withToken(get("/conversations/{id}/messages?limit=2", conversationId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(4))
                .andExpect(jsonPath("$[1].id").value(5));

        mockMvc.perform(withToken(get("/conversations/{id}/messages?before=4&limit=2", conversationId), token))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[1].id").value(3));

        mockMvc.perform(withToken(get("/conversations/{id}/messages?before=2&limit=2", conversationId), token))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("GET /messages: da mais recente para a mais antiga, com cursor")
    void messagesByCursor() throws Exception {
        mockMvc.perform(withToken(get("/messages?limit=3"), token))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[2].id").value(3));

        mockMvc.perform(withToken(get("/messages?before=3"), token))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    @DisplayName("conversas por página, a de mensagem mais recente primeiro")
    void conversationsByPage() throws Exception {
        mockMvc.perform(withToken(post("/messages"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson(null, 77, "João Lima", "NORMAL")))
                .andExpect(status().isCreated());

        mockMvc.perform(withToken(get("/conversations?size=1"), token))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].recipientName").value("João Lima"));
        mockMvc.perform(withToken(get("/conversations?page=1&size=1"), token))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].recipientName").value("Maria Souza"));
        mockMvc.perform(withToken(get("/conversations?page=2&size=1"), token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("listar mensagens de conversas diferentes não faz uma query por conversa (sem N+1 com LAZY)")
    void noNPlusOne() throws Exception {
        // Mais 4 conversas: com EAGER, cada conversa distinta viraria um SELECT a mais na listagem
        for (int recipient = 60; recipient < 64; recipient++) {
            mockMvc.perform(withToken(post("/messages"), token).contentType(MediaType.APPLICATION_JSON)
                            .content(messageJson(null, recipient, "Destinatário " + recipient, "NORMAL")))
                    .andExpect(status().isCreated());
        }
        // As estatisticas sao globais: espera o worker terminar para nao contar os updates de status dele
        await().until(() -> count("tb_messages") == jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tb_messages WHERE status = 'DELIVERED'", Integer.class));
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        statistics.clear();
        mockMvc.perform(withToken(get("/messages?limit=1"), token))
                .andExpect(jsonPath("$.length()").value(1));
        long oneMessage = statistics.getPrepareStatementCount();

        statistics.clear();
        mockMvc.perform(withToken(get("/messages"), token))
                .andExpect(jsonPath("$.length()").value(9));
        long nineMessagesInFiveConversations = statistics.getPrepareStatementCount();

        assertThat(nineMessagesInFiveConversations).isEqualTo(oneMessage);
    }
}
