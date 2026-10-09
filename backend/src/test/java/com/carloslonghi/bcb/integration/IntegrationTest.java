package com.carloslonghi.bcb.integration;

import com.carloslonghi.bcb.infra.queue.MessageQueue;
import com.carloslonghi.bcb.repository.MessageRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
@Import(IntegrationTest.MockMvcServletPath.class)
public abstract class IntegrationTest {

    // O TokenAuthenticationFilter decide as rotas publicas por getServletPath(). No Tomcat ele e o caminho
    // da requisicao (DispatcherServlet mapeado em "/"), mas no MockMvc vem vazio; aqui fica igual ao Tomcat
    @TestConfiguration
    static class MockMvcServletPath {
        @Bean
        MockMvcBuilderCustomizer servletPathAsInTomcat() {
            return builder -> builder.defaultRequest(get("/").with(request -> {
                request.setServletPath(request.getRequestURI());
                return request;
            }));
        }
    }

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @ServiceConnection
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3-management-alpine");

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
        jdbcTemplate.execute("TRUNCATE tb_messages, tb_conversations, tb_clients RESTART IDENTITY CASCADE");
    }

    protected Long createPrePaidClient(String document, String balance) throws Exception {
        return createClient(document, "PRE_PAID", balance, "0.00", true);
    }

    protected Long createPostPaidClient(String document, String limit) throws Exception {
        return createClient(document, "POST_PAID", "0.00", limit, true);
    }

    protected Long createClient(String document, String planType, String balance, String limit, boolean active) throws Exception {
        String body = """
                {"name": "Cliente %s", "document": "%s", "documentType": "%s", "planType": "%s",
                 "balance": %s, "limit": %s, "active": %s}
                """.formatted(document, document, document.length() == 11 ? "CPF" : "CNPJ", planType, balance, limit, active);

        String response = mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(response).read("$.id", Long.class);
    }

    protected String login(String document) throws Exception {
        String response = mockMvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"document\": \"%s\"}".formatted(document)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
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
