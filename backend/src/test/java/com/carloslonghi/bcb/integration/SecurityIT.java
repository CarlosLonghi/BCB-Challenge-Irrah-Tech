package com.carloslonghi.bcb.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIT extends IntegrationTest {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    @DisplayName("rota protegida sem token responde 401")
    void withoutToken() throws Exception {
        mockMvc.perform(get("/messages"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Sessão não encontrada. Faça login para continuar."));
    }

    @Test
    @DisplayName("token que não é JWT responde 401")
    void invalidToken() throws Exception {
        mockMvc.perform(withToken(get("/messages"), "token-que-nao-existe"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Sessão inválida ou expirada. Faça login novamente."));
    }

    @Test
    @DisplayName("JWT com payload adulterado (outro cliente) responde 401")
    void tamperedToken() throws Exception {
        createPrePaidClient("12345678909", "10.00");
        String[] parts = login("12345678909").split("\\.");
        String otherPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"2\",\"exp\":4102444800}".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(withToken(get("/messages"), parts[0] + "." + otherPayload + "." + parts[2]))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Sessão inválida ou expirada. Faça login novamente."));
    }

    @Test
    @DisplayName("JWT vencido responde 401")
    void expiredToken() throws Exception {
        Long clientId = createPrePaidClient("12345678909", "10.00");
        Instant past = Instant.now().minus(Duration.ofDays(1));
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().subject(clientId.toString())
                        .issuedAt(past).expiresAt(past.plus(Duration.ofHours(1))).build()
        )).getTokenValue();

        mockMvc.perform(withToken(get("/messages"), token))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Sessão inválida ou expirada. Faça login novamente."));
    }

    @Test
    @DisplayName("esquema Bearer não diferencia maiúsculas (RFC 6750)")
    void lowercaseBearer() throws Exception {
        createPrePaidClient("12345678909", "10.00");
        String token = login("12345678909");

        mockMvc.perform(get("/messages").header("Authorization", "bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("header Authorization de outro esquema responde 401 de sessão não encontrada")
    void basicAuthHeader() throws Exception {
        mockMvc.perform(get("/messages").header("Authorization", "Basic YWJjOmRlZg=="))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Sessão não encontrada. Faça login para continuar."));
    }

    @Test
    @DisplayName("token válido acessa as rotas protegidas e só vê o próprio cliente")
    void validToken() throws Exception {
        Long clientId = createPrePaidClient("12345678909", "10.00");
        createPrePaidClient("98765432100", "10.00");
        String token = login("12345678909");

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
        mockMvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON).content(authJson("00000000000", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Documento ou senha inválidos."));
    }

    @Test
    @DisplayName("login com senha errada responde 401 com a mesma mensagem")
    void wrongPassword() throws Exception {
        createPrePaidClient("12345678909", "10.00");

        mockMvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON).content(authJson("12345678909", "senha-errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Documento ou senha inválidos."));
    }

    @Test
    @DisplayName("login de cliente inativo responde 403")
    void inactiveClient() throws Exception {
        createClient("12345678909", "PRE_PAID", "10.00", "0.00", false);

        mockMvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON).content(authJson("12345678909", PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Cliente inativo."));
    }

    @Test
    @DisplayName("login sem documento responde 400")
    void blankDocument() throws Exception {
        mockMvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON).content(authJson("", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Documento é obrigatório"));
    }
}
