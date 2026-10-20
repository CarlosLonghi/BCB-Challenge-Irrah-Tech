package com.carloslonghi.bcb.integration;

import com.carloslonghi.bcb.config.TraceIdHeaderFilter;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// /actuator/prometheus com as metricas proprias e o header X-Trace-Id
class ObservabilityIT extends IntegrationTest {

    private static final String TRACE_ID = "[0-9a-f]{16,32}";

    private String prometheus() throws Exception {
        return mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("métricas de envio, entrega e fila aparecem no /actuator/prometheus")
    void customMetrics() throws Exception {
        createPrePaidClient("12345678909", "10.00");
        String token = login("12345678909");
        String sent = mockMvc.perform(withToken(post("/messages"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson(null, 55, "Maria Souza", "URGENT")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long messageId = JsonPath.parse(sent).read("$.id", Long.class);
        await().until(() -> "DELIVERED".equals(
                jdbcTemplate.queryForObject("SELECT status FROM tb_messages WHERE id = ?", String.class, messageId)));

        await().untilAsserted(() -> assertThat(prometheus())
                .contains("bcb_messages_sent_total{priority=\"URGENT\"}")
                .contains("bcb_messages_delivery_seconds_count{priority=\"URGENT\"}")
                // Registrada no startup: existe mesmo sem falha neste teste (o valor depende dos outros ITs,
                // que dividem o contexto; o zero inicial fica no MessageMetricsTest)
                .contains("bcb_messages_failed_total{priority=\"NORMAL\"} ")
                // Histograma do HTTP (management.metrics.distribution.percentiles-histogram.http.server.requests)
                .contains("http_server_requests_seconds_bucket{")
                .contains("bcb_queue_messages{queue=\"bcb.messages\"}")
                .contains("bcb_queue_messages{queue=\"bcb.messages.dlq\"}")
                .contains("bcb_events_connections"));
    }

    @Test
    @DisplayName("toda resposta traz X-Trace-Id, inclusive o 401 do Spring Security")
    void traceIdHeader() throws Exception {
        mockMvc.perform(get("/conversations"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(TraceIdHeaderFilter.HEADER, matchesPattern(TRACE_ID)));

        createPrePaidClient("12345678909", "10.00");
        mockMvc.perform(withToken(get("/conversations"), login("12345678909")))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceIdHeaderFilter.HEADER, matchesPattern(TRACE_ID)));
    }
}
