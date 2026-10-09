package com.carloslonghi.bcb.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// Sintoma de negocio do lock: envios simultaneos nao podem gastar o mesmo saldo duas vezes.
// E uma corrida, entao sem o lock a falha pode nao aparecer em toda execucao; o ClientRepositoryLockIT e o teste deterministico
class MessageSendConcurrencyIT extends IntegrationTest {

    private static final int REQUESTS = 8;

    @Test
    @DisplayName("envios simultâneos com saldo para uma só mensagem: só um é cobrado, os outros recebem 402")
    void concurrentSendsDoNotDoubleSpend() throws Exception {
        Long clientId = createPrePaidClient("12345678901", "0.25");
        String token = login("12345678901");
        String body = messageJson(null, 55, "Maria Souza", "NORMAL");

        ExecutorService executor = Executors.newFixedThreadPool(REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> responses = new ArrayList<>();
        try {
            for (int i = 0; i < REQUESTS; i++) {
                responses.add(executor.submit(() -> {
                    start.await();
                    return mockMvc.perform(withToken(post("/messages"), token)
                                    .contentType(MediaType.APPLICATION_JSON).content(body))
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> response : responses) {
                statuses.add(response.get(30, TimeUnit.SECONDS));
            }

            assertThat(statuses).containsOnly(201, 402);
            assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        } finally {
            executor.shutdownNow();
        }

        assertThat(balanceOf(clientId)).isEqualByComparingTo("0");
        assertThat(count("tb_messages")).isEqualTo(1);
    }
}
