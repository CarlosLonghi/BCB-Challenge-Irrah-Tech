package com.carloslonghi.bcb.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Fluxo completo: HTTP -> debito no Postgres -> RabbitMQ -> worker -> status DELIVERED
class MessageFlowIT extends IntegrationTest {

    private String send(String token, String body, int expectedStatus) throws Exception {
        return mockMvc.perform(withToken(post("/messages"), token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    private String statusOf(String token, Long messageId) throws Exception {
        return mockMvc.perform(withToken(get("/messages/" + messageId + "/status"), token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString().replace("\"", "");
    }

    @Test
    @DisplayName("pré-pago: NORMAL custa 0,25 e URGENT 0,50, e o worker entrega a mensagem")
    void prePaidSendIsChargedAndDelivered() throws Exception {
        Long clientId = createPrePaidClient("12345678901", "10.00");
        String token = login("12345678901");

        String first = send(token, messageJson(null, 55, "Maria Souza", "NORMAL"), 201);
        assertThat(JsonPath.parse(first).read("$.cost", Double.class)).isEqualTo(0.25);
        assertThat(JsonPath.parse(first).read("$.currentBalance", Double.class)).isEqualTo(9.75);
        assertThat(JsonPath.<String>read(first, "$.status")).isEqualTo("QUEUED");

        Long conversationId = JsonPath.parse(first).read("$.conversationId", Long.class);
        String second = send(token, messageJson(conversationId, null, null, "URGENT"), 201);
        assertThat(JsonPath.parse(second).read("$.currentBalance", Double.class)).isEqualTo(9.25);
        assertThat(JsonPath.parse(second).read("$.conversationId", Long.class)).isEqualTo(conversationId);

        assertThat(balanceOf(clientId)).isEqualByComparingTo("9.25");
        assertThat(count("tb_conversations")).isEqualTo(1);

        Long messageId = JsonPath.parse(second).read("$.id", Long.class);
        await().untilAsserted(() -> assertThat(statusOf(token, messageId)).isEqualTo("DELIVERED"));
    }

    @Test
    @DisplayName("pré-pago sem saldo: 402, saldo intacto e nenhuma mensagem gravada")
    void prePaidWithoutBalance() throws Exception {
        Long clientId = createPrePaidClient("12345678901", "0.40");
        String token = login("12345678901");

        String body = send(token, messageJson(null, 55, "Maria Souza", "URGENT"), 402);

        assertThat(JsonPath.<String>read(body, "$.message")).isEqualTo("Saldo insuficiente.");
        assertThat(balanceOf(clientId)).isEqualByComparingTo("0.40");
        assertThat(count("tb_messages")).isZero();
        assertThat(count("tb_conversations")).isZero();
    }

    @Test
    @DisplayName("pós-pago: consome o limite e, quando acaba, responde 402")
    void postPaidConsumesLimit() throws Exception {
        Long clientId = createPostPaidClient("12345678000199", "0.50");
        String token = login("12345678000199");

        String first = send(token, messageJson(null, 55, "Maria Souza", "URGENT"), 201);
        assertThat(JsonPath.parse(first).read("$.currentBalance", Double.class)).isEqualTo(0.0);
        assertThat(limitOf(clientId)).isEqualByComparingTo("0");

        Long conversationId = JsonPath.parse(first).read("$.conversationId", Long.class);
        String refused = send(token, messageJson(conversationId, null, null, "NORMAL"), 402);
        assertThat(JsonPath.<String>read(refused, "$.message")).isEqualTo("Limite de consumo excedido.");
        assertThat(count("tb_messages")).isEqualTo(1);
    }

    @Test
    @DisplayName("conversa nova sem destinatário responde 400 sem cobrar")
    void missingRecipient() throws Exception {
        Long clientId = createPrePaidClient("12345678901", "10.00");
        String token = login("12345678901");

        send(token, messageJson(null, null, null, "NORMAL"), 400);

        assertThat(balanceOf(clientId)).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("mensagem sem conteúdo ou prioridade responde 400")
    void invalidMessage() throws Exception {
        createPrePaidClient("12345678901", "10.00");
        String token = login("12345678901");

        String body = send(token, "{\"recipientId\": 55, \"recipientName\": \"Maria\", \"content\": \"\"}", 400);

        assertThat(JsonPath.<String>read(body, "$.message"))
                .isEqualTo("Mensagem não pode ser vazia; Prioridade é obrigatória (NORMAL ou URGENT)");
    }

    @Test
    @DisplayName("conversa e mensagem de outro cliente contam como inexistentes (404), sem cobrar")
    void otherClientResources() throws Exception {
        createPrePaidClient("12345678901", "10.00");
        Long otherId = createPrePaidClient("98765432100", "10.00");
        String token = login("12345678901");
        String otherToken = login("98765432100");

        String otherMessage = send(otherToken, messageJson(null, 55, "Maria Souza", "NORMAL"), 201);
        Long otherConversation = JsonPath.parse(otherMessage).read("$.conversationId", Long.class);
        Long otherMessageId = JsonPath.parse(otherMessage).read("$.id", Long.class);

        send(token, messageJson(otherConversation, null, null, "NORMAL"), 404);
        mockMvc.perform(withToken(get("/conversations/" + otherConversation), token)).andExpect(status().isNotFound());
        mockMvc.perform(withToken(get("/conversations/" + otherConversation + "/messages"), token)).andExpect(status().isNotFound());
        mockMvc.perform(withToken(get("/messages/" + otherMessageId), token)).andExpect(status().isNotFound());

        assertThat(balanceOf(otherId)).isEqualByComparingTo("9.75");
        mockMvc.perform(withToken(get("/messages"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("conversas e status da fila refletem as mensagens enviadas")
    void conversationsAndQueueStatus() throws Exception {
        createPrePaidClient("12345678901", "10.00");
        String token = login("12345678901");

        String sent = send(token, messageJson(null, 55, "Maria Souza", "URGENT"), 201);
        Long conversationId = JsonPath.parse(sent).read("$.conversationId", Long.class);
        Long messageId = JsonPath.parse(sent).read("$.id", Long.class);
        await().untilAsserted(() -> assertThat(statusOf(token, messageId)).isEqualTo("DELIVERED"));

        mockMvc.perform(withToken(get("/conversations"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(conversationId))
                .andExpect(jsonPath("$[0].recipientName").value("Maria Souza"))
                .andExpect(jsonPath("$[0].lastMessageContent").value("Olá!"));
        mockMvc.perform(withToken(get("/conversations/" + conversationId + "/messages"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(messageId))
                .andExpect(jsonPath("$[0].status").value("DELIVERED"));
        mockMvc.perform(withToken(get("/queue/status"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queuedTotal").value(0))
                .andExpect(jsonPath("$.delivered").value(1));
    }
}
