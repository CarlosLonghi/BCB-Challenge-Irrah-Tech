package com.carloslonghi.bcb.config;

import com.carloslonghi.bcb.service.AuthService;
import com.carloslonghi.bcb.support.AuthenticatedClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenAuthenticationFilterTest {

    @Mock
    private AuthService authService;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private TokenAuthenticationFilter filter;

    @AfterEach
    void tearDown() {
        AuthenticatedClient.logout();
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

    @Test
    @DisplayName("token válido vira o principal do contexto e a requisição segue")
    void validToken() throws ServletException, IOException {
        MockHttpServletRequest request = request("GET", "/messages");
        request.addHeader("Authorization", "Bearer abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authService.getClientIdFromToken("abc")).thenReturn(1L);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(1L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("sem header Authorization responde 401")
    void missingHeader() throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/messages"), response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getErrorMessage()).isEqualTo("Sessão não encontrada. Faça login para continuar.");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("header sem o prefixo Bearer responde 401")
    void headerWithoutBearer() throws ServletException, IOException {
        MockHttpServletRequest request = request("GET", "/messages");
        request.addHeader("Authorization", "Basic abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("token desconhecido responde 401 sem autenticar")
    void unknownToken() throws ServletException, IOException {
        MockHttpServletRequest request = request("GET", "/messages");
        request.addHeader("Authorization", "Bearer expirado");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authService.getClientIdFromToken("expirado")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getErrorMessage()).isEqualTo("Sessão inválida ou expirada. Faça login novamente.");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(any(), any());
    }

    @ParameterizedTest(name = "{0} {1} -> público={2}")
    @CsvSource({
            "POST,    /auth,                     true",
            "GET,     /actuator/health,          true",
            "POST,    /clients,                  true",
            "OPTIONS, /messages,                 true",
            "GET,     /v3/api-docs/swagger,      true",
            "GET,     /swagger-ui/index.html,    true",
            "GET,     /swagger-ui.html,          true",
            "GET,     /webjars/x.js,             true",
            "GET,     /clients,                  false",
            "PUT,     /clients/1,                false",
            "POST,    /messages,                 false"
    })
    @DisplayName("só as rotas públicas dispensam o token")
    void publicRoutes(String method, String path, boolean isPublic) {
        assertThat(filter.shouldNotFilter(request(method, path))).isEqualTo(isPublic);
    }

    @ParameterizedTest(name = "Authorization: ''{0}''")
    @CsvSource(value = {"'Bearer '", "bearer abc", "Bearerabc", "''"})
    @DisplayName("header malformado ou com token vazio responde 401 sem autenticar")
    void malformedHeader(String header) throws ServletException, IOException {
        // Mock de Long devolve 0 por padrao; o AuthService real devolve null para token vazio
        lenient().when(authService.getClientIdFromToken("")).thenReturn(null);
        MockHttpServletRequest request = request("GET", "/messages");
        request.addHeader("Authorization", header);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(any(), any());
    }
}
