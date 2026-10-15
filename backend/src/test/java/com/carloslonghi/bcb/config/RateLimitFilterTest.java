package com.carloslonghi.bcb.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private static final RateLimitProperties.Rule TWO_PER_MINUTE = new RateLimitProperties.Rule(2, Duration.ofMinutes(1));

    private final RateLimitFilter filter =
            new RateLimitFilter(new RateLimitProperties(true, TWO_PER_MINUTE, new RateLimitProperties.Rule(1, Duration.ofHours(1))));

    private static MockHttpServletRequest request(String method, String uri, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(ip);
        return request;
    }

    private MockHttpServletResponse call(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    @DisplayName("login: passa até a capacidade e, estourou, responde 429 com Retry-After e mensagem")
    void blocksAfterCapacity() throws Exception {
        assertThat(call(request("POST", "/auth", "10.0.0.1")).getStatus()).isEqualTo(200);
        assertThat(call(request("POST", "/auth", "10.0.0.1")).getStatus()).isEqualTo(200);

        MockHttpServletResponse blocked = call(request("POST", "/auth", "10.0.0.1"));

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(Long.parseLong(blocked.getHeader("Retry-After"))).isBetween(1L, 60L);
        assertThat(blocked.getErrorMessage()).startsWith("Muitas tentativas. Aguarde ");
    }

    @Test
    @DisplayName("cada IP e cada rota têm o próprio bucket")
    void bucketPerIpAndRoute() throws Exception {
        call(request("POST", "/auth", "10.0.0.1"));
        call(request("POST", "/auth", "10.0.0.1"));

        assertThat(call(request("POST", "/auth", "10.0.0.2")).getStatus()).isEqualTo(200);
        assertThat(call(request("POST", "/clients", "10.0.0.1")).getStatus()).isEqualTo(200);
        assertThat(call(request("POST", "/clients", "10.0.0.1")).getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("outras rotas e métodos não são limitados")
    void otherRoutesPass() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(call(request("GET", "/clients", "10.0.0.1")).getStatus()).isEqualTo(200);
            assertThat(call(request("POST", "/messages", "10.0.0.1")).getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("desligado: nada é limitado")
    void disabled() throws Exception {
        RateLimitFilter off = new RateLimitFilter(new RateLimitProperties(false, TWO_PER_MINUTE, TWO_PER_MINUTE));

        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            off.doFilter(request("POST", "/auth", "10.0.0.1"), response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
