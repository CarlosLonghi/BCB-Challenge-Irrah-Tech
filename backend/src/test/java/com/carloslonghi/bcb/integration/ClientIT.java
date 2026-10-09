package com.carloslonghi.bcb.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClientIT extends IntegrationTest {

    private static String clientJson(String document, String documentType, String balance) {
        return """
                {"name": "Ana", "document": "%s", "documentType": "%s", "planType": "PRE_PAID",
                 "balance": %s, "limit": 0, "active": true}
                """.formatted(document, documentType, balance);
    }

    @Test
    @DisplayName("cadastro responde 201 e grava o cliente")
    void create() throws Exception {
        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON).content(clientJson("12345678901", "CPF", "10.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.balance").value(10.00))
                .andExpect(jsonPath("$.planType").value("PRE_PAID"));

        assertThat(count("tb_clients")).isEqualTo(1);
    }

    @Test
    @DisplayName("documento já cadastrado responde 409")
    void duplicatedDocument() throws Exception {
        createPrePaidClient("12345678901", "10.00");

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON).content(clientJson("12345678901", "CPF", "1.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Já existe cliente com o documento 12345678901."));
        assertThat(count("tb_clients")).isEqualTo(1);
    }

    @Test
    @DisplayName("CPF com 14 dígitos responde 400")
    void documentDoesNotMatchType() throws Exception {
        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON).content(clientJson("12345678000199", "CPF", "1.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CPF deve ter 11 dígitos."));
    }

    @Test
    @DisplayName("payload inválido responde 400 com as mensagens de todos os campos")
    void invalidPayload() throws Exception {
        String body = """
                {"name": "", "document": "12345678901", "documentType": "CPF", "planType": "PRE_PAID",
                 "balance": -1, "limit": 0}
                """;

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nome é obrigatório; Saldo não pode ser negativo"));
        assertThat(count("tb_clients")).isZero();
    }

    @Test
    @DisplayName("enum desconhecido no JSON responde 400")
    void unknownEnum() throws Exception {
        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON).content(clientJson("12345678901", "RG", "1.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição inválido. Confira o JSON e os valores dos campos."));
    }

    @Test
    @DisplayName("consultar, alterar ou ver o saldo de outro cliente responde 403")
    void otherClientForbidden() throws Exception {
        createPrePaidClient("12345678901", "10.00");
        Long otherId = createPrePaidClient("98765432100", "10.00");
        String token = login("12345678901");

        mockMvc.perform(withToken(get("/clients/" + otherId), token)).andExpect(status().isForbidden());
        mockMvc.perform(withToken(get("/clients/" + otherId + "/balance"), token)).andExpect(status().isForbidden());
        mockMvc.perform(withToken(put("/clients/" + otherId), token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Hacker\"}"))
                .andExpect(status().isForbidden());

        assertThat(jdbcTemplate.queryForObject("SELECT name FROM tb_clients WHERE id = ?", String.class, otherId))
                .isEqualTo("Cliente 98765432100");
    }

    @Test
    @DisplayName("PUT altera só o nome, mesmo com saldo, limite e plano no JSON")
    void updateChangesOnlyName() throws Exception {
        Long clientId = createPrePaidClient("12345678901", "10.00");
        String token = login("12345678901");

        mockMvc.perform(withToken(put("/clients/" + clientId), token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Novo Nome\", \"balance\": 9999, \"limit\": 9999, \"planType\": \"POST_PAID\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Novo Nome"));

        assertThat(balanceOf(clientId)).isEqualByComparingTo("10.00");
        assertThat(limitOf(clientId)).isEqualByComparingTo("0");
        assertThat(jdbcTemplate.queryForObject("SELECT plan_type FROM tb_clients WHERE id = ?", String.class, clientId))
                .isEqualTo("PRE_PAID");
    }

    @Test
    @DisplayName("GET /balance devolve saldo e limite do próprio cliente")
    void ownBalance() throws Exception {
        Long clientId = createPostPaidClient("12345678901", "50.00");
        String token = login("12345678901");

        mockMvc.perform(withToken(get("/clients/" + clientId + "/balance"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.limit").value(50.00));
    }
}
