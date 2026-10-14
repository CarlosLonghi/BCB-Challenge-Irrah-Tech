package com.carloslonghi.bcb.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O contrato da API fica versionado em frontend/openapi.json, de onde saem os tipos do frontend
 * (npm run api:types). Este teste falha quando o backend muda o contrato e o arquivo nao acompanha.
 * Para regravar: sh ./mvnw verify -Dit.test=OpenApiContractIT -Dopenapi.write=true
 */
class OpenApiContractIT extends IntegrationTest {

    private static final Path SPEC = Path.of("..", "frontend", "openapi.json");
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    @Test
    @DisplayName("frontend/openapi.json é igual ao /v3/api-docs")
    void specIsUpToDate() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        ObjectNode live = (ObjectNode) JSON.readTree(body);
        // A URL do servidor depende de onde a aplicacao roda; nao faz parte do contrato
        live.remove("servers");

        if (Boolean.getBoolean("openapi.write")) {
            Files.writeString(SPEC, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(live) + "\n");
            return;
        }

        JsonNode committed = JSON.readTree(Files.readString(SPEC));
        assertThat(committed)
                .as("frontend/openapi.json desatualizado: rode sh ./mvnw verify -Dit.test=OpenApiContractIT "
                        + "-Dopenapi.write=true e depois npm run api:types no frontend")
                .isEqualTo(live);
    }
}
