package com.carloslonghi.bcb.integration;

import com.carloslonghi.bcb.infra.queue.MessageQueue;
import com.carloslonghi.bcb.repository.MessageRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integracao: sobe a aplicacao inteira com Postgres e RabbitMQ reais (Testcontainers).
 * Os containers sao iniciados uma vez e reaproveitados por todas as classes; todas usam o mesmo contexto
 * do Spring (os spies ficam aqui para nao criar contextos diferentes, cada um com seu consumidor na fila).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
public abstract class IntegrationTest {

    protected static final String PASSWORD = "senha-forte-123";

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @ServiceConnection
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4-management-alpine");

    static {
        POSTGRES.start();
        RABBIT.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    // Spies: comportamento real, mas o teste pode forcar uma falha ou verificar chamadas (resetados apos cada teste)
    @MockitoSpyBean
    protected MessageRepository messageRepository;

    @MockitoSpyBean
    protected MessageQueue messageQueue;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE tb_ledger_entries, tb_messages, tb_conversations, tb_clients RESTART IDENTITY CASCADE");
    }

    protected Long createPrePaidClient(String document, String balance) throws Exception {
        return createClient(document, "PRE_PAID", balance, "0.00", true);
    }

    protected Long createPostPaidClient(String document, String limit) throws Exception {
        return createClient(document, "POST_PAID", "0.00", limit, true);
    }

    protected Long createClient(String document, String planType, String balance, String limit, boolean active) throws Exception {
        String body = """
                {"name": "Cliente %s", "document": "%s", "password": "%s", "documentType": "%s", "planType": "%s",
                 "balance": %s, "limit": %s, "active": %s}
                """.formatted(document, document, PASSWORD, document.length() == 11 ? "CPF" : "CNPJ", planType, balance, limit, active);

        String response = mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(response).read("$.id", Long.class);
    }

    protected String login(String document) throws Exception {
        String response = mockMvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON)
                        .content(authJson(document, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }

    protected static String authJson(String document, String password) {
        return "{\"document\": \"%s\", \"password\": \"%s\"}".formatted(document, password);
    }

    protected static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    protected static String messageJson(Long conversationId, Integer recipientId, String recipientName, String priority) {
        return """
                {"conversationId": %s, "recipientId": %s, "recipientName": %s, "content": "Olá!", "priority": "%s"}
                """.formatted(conversationId, recipientId, recipientName == null ? "null" : "\"" + recipientName + "\"", priority);
    }

    protected BigDecimal balanceOf(Long clientId) {
        return jdbcTemplate.queryForObject("SELECT balance FROM tb_clients WHERE id = ?", BigDecimal.class, clientId);
    }

    protected BigDecimal limitOf(Long clientId) {
        return jdbcTemplate.queryForObject("SELECT client_limit FROM tb_clients WHERE id = ?", BigDecimal.class, clientId);
    }

    protected int count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }
}
