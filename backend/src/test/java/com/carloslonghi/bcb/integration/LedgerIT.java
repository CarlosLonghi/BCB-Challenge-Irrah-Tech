package com.carloslonghi.bcb.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LedgerIT extends IntegrationTest {

    @Test
    @DisplayName("extrato lista um DEBIT por envio, do mais recente para o mais antigo")
    void listsDebits() throws Exception {
        Long clientId = createPrePaidClient("12345678909", "10.00");
        String token = login("12345678909");
        for (String priority : new String[]{"NORMAL", "URGENT"}) {
            mockMvc.perform(withToken(post("/messages"), token).contentType(MediaType.APPLICATION_JSON)
                            .content(messageJson(null, 55, "Maria Souza", priority)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(withToken(get("/clients/{id}/ledger", clientId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("DEBIT"))
                .andExpect(jsonPath("$[0].amount").value(0.50))
                .andExpect(jsonPath("$[0].messageId").value(2))
                .andExpect(jsonPath("$[1].amount").value(0.25))
                .andExpect(jsonPath("$[1].messageId").value(1));
    }

    @Test
    @DisplayName("extrato de outro cliente responde 403")
    void otherClientLedger() throws Exception {
        createPrePaidClient("12345678909", "10.00");
        Long otherId = createPrePaidClient("98765432100", "10.00");
        String token = login("12345678909");

        mockMvc.perform(withToken(get("/clients/{id}/ledger", otherId), token))
                .andExpect(status().isForbidden());
    }
}
