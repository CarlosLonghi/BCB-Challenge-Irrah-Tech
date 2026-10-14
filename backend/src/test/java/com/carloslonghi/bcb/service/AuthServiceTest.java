package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.config.JwtConfig;
import com.carloslonghi.bcb.config.JwtProperties;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.exception.ClientNotActiveException;
import com.carloslonghi.bcb.exception.InvalidCredentialsException;
import com.carloslonghi.bcb.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PASSWORD = "senha-forte-123";
    // BCrypt com custo baixo: o teste nao precisa da lentidao do custo padrao
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @Mock
    private ClientRepository clientRepository;

    private JwtConfig jwtConfig;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties("segredo-dos-testes-unitarios-0123456789", Duration.ofHours(8));
        jwtConfig = new JwtConfig(properties);
        authService = new AuthService(clientRepository, ENCODER, jwtConfig.jwtEncoder(), properties, Clock.systemUTC());
    }

    private Client clientWithPassword() {
        Client client = prePaidClient(1L, "10.00");
        client.setPasswordHash(ENCODER.encode(PASSWORD));
        return client;
    }

    @Test
    @DisplayName("authenticate devolve o cliente ativo quando documento e senha conferem")
    void authenticatesActiveClient() {
        Client client = clientWithPassword();
        when(clientRepository.findByDocument("12345678909")).thenReturn(Optional.of(client));

        assertThat(authService.authenticate("12345678909", PASSWORD)).isSameAs(client);
    }

    @Test
    @DisplayName("authenticate recusa senha errada com a mesma mensagem de documento inexistente")
    void rejectsWrongPassword() {
        when(clientRepository.findByDocument("12345678909")).thenReturn(Optional.of(clientWithPassword()));

        assertThatThrownBy(() -> authService.authenticate("12345678909", "outra-senha"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Documento ou senha inválidos.");
    }

    @Test
    @DisplayName("authenticate recusa documento não cadastrado")
    void rejectsUnknownDocument() {
        when(clientRepository.findByDocument("00000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate("00000000000", PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Documento ou senha inválidos.");
    }

    @Test
    @DisplayName("cliente cadastrado antes da senha existir não consegue entrar")
    void rejectsClientWithoutPassword() {
        when(clientRepository.findByDocument("12345678909")).thenReturn(Optional.of(prePaidClient(1L, "10.00")));

        assertThatThrownBy(() -> authService.authenticate("12345678909", PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("authenticate recusa cliente inativo mesmo com a senha certa")
    void rejectsInactiveClient() {
        Client client = clientWithPassword();
        client.setActive(false);
        when(clientRepository.findByDocument("12345678909")).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> authService.authenticate("12345678909", PASSWORD))
                .isInstanceOf(ClientNotActiveException.class)
                .hasMessage("Cliente inativo.");
    }

    @Test
    @DisplayName("o token é um JWT com o id do cliente no sub e expiração conforme o ttl")
    void tokenCarriesClientId() {
        JwtDecoder decoder = jwtConfig.jwtDecoder();

        Jwt jwt = decoder.decode(authService.createToken(1L));

        assertThat(jwt.getSubject()).isEqualTo("1");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(8));
    }

    @Test
    @DisplayName("token vencido é recusado pelo decoder")
    void expiredToken() {
        JwtProperties properties = new JwtProperties("segredo-dos-testes-unitarios-0123456789", Duration.ofMinutes(5));
        Clock yesterday = Clock.fixed(Instant.now().minus(Duration.ofDays(1)), ZoneOffset.UTC);
        AuthService pastService = new AuthService(clientRepository, ENCODER, jwtConfig.jwtEncoder(), properties, yesterday);

        String token = pastService.createToken(1L);

        assertThatThrownBy(() -> jwtConfig.jwtDecoder().decode(token))
                .isInstanceOf(JwtValidationException.class);
    }
}
