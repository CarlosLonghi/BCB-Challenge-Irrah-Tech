package com.carloslonghi.bcb.integration;

import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.infra.worker.ReadSimulator;
import com.carloslonghi.bcb.service.MessageService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GET /events: o status de cada mensagem chega por SSE ao dono, e só a ele
class EventStreamIT extends IntegrationTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ReadSimulator readSimulator;

    private MockHttpServletResponse openStream(String token) throws Exception {
        return mockMvc.perform(withToken(get("/events"), token).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn().getResponse();
    }

    private Long send(String token) throws Exception {
        String body = mockMvc.perform(withToken(post("/messages"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson(null, 55, "Maria Souza", "NORMAL")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(body).read("$.id", Long.class);
    }

    private static String statusEvent(Long messageId, MessageStatus status) {
        return "event:message-status\ndata:{\"messageId\":%d,\"conversationId\":1,\"status\":\"%s\"}"
                .formatted(messageId, status);
    }

    @Test
    @DisplayName("sem token: 401")
    void requiresToken() throws Exception {
        mockMvc.perform(get("/events")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("o dono recebe ready e cada etapa do worker, em ordem; outro cliente não recebe nada")
    void ownerReceivesWorkerSteps() throws Exception {
        createPrePaidClient("12345678909", "10.00");
        createPrePaidClient("52998224725", "10.00");
        String token = login("12345678909");
        MockHttpServletResponse owner = openStream(token);
        MockHttpServletResponse other = openStream(login("52998224725"));

        Long messageId = send(token);

        await().untilAsserted(() -> assertThat(owner.getContentAsString())
                .contains(statusEvent(messageId, MessageStatus.DELIVERED)));
        String content = owner.getContentAsString();
        assertThat(content).startsWith("event:ready\n");
        assertThat(content.indexOf(statusEvent(messageId, MessageStatus.PROCESSING)))
                .isPositive()
                .isLessThan(content.indexOf(statusEvent(messageId, MessageStatus.SENT)));
        assertThat(other.getContentAsString()).doesNotContain("message-status");
    }

    @Test
    @DisplayName("a leitura simulada avisa o READ")
    void readSimulatorPublishesRead() throws Exception {
        createPrePaidClient("12345678909", "10.00");
        String token = login("12345678909");
        MockHttpServletResponse stream = openStream(token);
        Long messageId = send(token);
        await().untilAsserted(() -> assertThat(stream.getContentAsString())
                .contains(statusEvent(messageId, MessageStatus.DELIVERED)));

        ReflectionTestUtils.setField(readSimulator, "readAfter", Duration.ZERO);
        try {
            readSimulator.markDeliveredAsRead();
        } finally {
            ReflectionTestUtils.setField(readSimulator, "readAfter", Duration.ofSeconds(4));
        }

        assertThat(stream.getContentAsString()).contains(statusEvent(messageId, MessageStatus.READ));
    }

    @Test
    @DisplayName("o estorno avisa o FAILED depois do commit")
    void refundPublishesFailed() throws Exception {
        createPrePaidClient("12345678909", "10.00");
        String token = login("12345678909");
        MockHttpServletResponse stream = openStream(token);
        // Publicação falhando: a mensagem fica QUEUED e o worker não mexe nela
        doThrow(new IllegalStateException("fila fora do ar")).when(messageQueue).enqueue(any());
        Long messageId = send(token);

        messageService.failAndRefund(messageId);

        assertThat(stream.getContentAsString()).contains(statusEvent(messageId, MessageStatus.FAILED));
    }
}
