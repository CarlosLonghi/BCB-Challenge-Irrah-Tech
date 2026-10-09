package com.carloslonghi.bcb.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIT extends IntegrationTest {

    @Test
    @DisplayName("rota protegida sem token responde 401")
    void withoutToken() throws Exception {
        mockMvc.perform(get("/messages"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Sessão não encontrada. Faça login para continuar."));
    }

    @Test
    @DisplayName("token inexistente responde 401")
    void invalidToken() throws Exception {
        mockMvc.perform(withToken(get("/messages"), "token-que-nao-existe"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Sessão inválida ou expirada. Faça login novamente."));
    }

    @Test
    @DisplayName("header com 'bearer' minúsculo não é aceito")
    void lowercaseBearer() throws Exception {
        createPrePaidClient("12345678901", "10.00");
        String token = login("12345678901");

        mockMvc.perform(get("/messages").header("Authorization", "bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token válido acessa as rotas protegidas e só vê o próprio cliente")
    void validToken() throws Exception {
        Long clientId = createPrePaidClient("12345678901", "10.00");
        createPrePaidClient("98765432100", "10.00");
        String token = login("12345678901");

        mockMvc.perform(withToken(get("/clients"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(clientId));
    }

    @Test
    @DisplayName("rotas públicas respondem sem token")
    void publicRoutes() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("login com documento não cadastrado responde 401")
    void unknownDocument() throws Exception {
        mockMvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON).content("{\"document\": \"00000000000\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Documento não cadastrado. Verifique o CPF/CNPJ."));
    }

    @Test
    @DisplayName("login de cliente inativo responde 403")
    void inactiveClient() throws Exception {
        createClient("12345678901", "PRE_PAID", "10.00", "0.00", false);

        mockMvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON).content("{\"document\": \"12345678901\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Cliente inativo."));
    }

    @Test
    @DisplayName("login sem documento responde 400")
    void blankDocument() throws Exception {
        mockMvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON).content("{\"document\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Documento é obrigatório"));
    }
}
