package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.exception.ClientNotActiveException;
import com.carloslonghi.bcb.exception.DocumentNotRegisteredException;
import com.carloslonghi.bcb.repository.ClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("authenticate devolve o cliente ativo do documento")
    void authenticatesActiveClient() {
        Client client = prePaidClient(1L, "10.00");
        when(clientRepository.findByDocument("12345678901")).thenReturn(Optional.of(client));

        assertThat(authService.authenticate("12345678901")).isSameAs(client);
    }

    @Test
    @DisplayName("authenticate recusa cliente inativo")
    void rejectsInactiveClient() {
        Client client = prePaidClient(1L, "10.00");
        client.setActive(false);
        when(clientRepository.findByDocument("12345678901")).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> authService.authenticate("12345678901"))
                .isInstanceOf(ClientNotActiveException.class);
    }

    @Test
    @DisplayName("authenticate recusa documento não cadastrado")
    void rejectsUnknownDocument() {
        when(clientRepository.findByDocument("00000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate("00000000000"))
                .isInstanceOf(DocumentNotRegisteredException.class);
    }

    @Test
    @DisplayName("o token criado resolve para o id do cliente; cada login gera um token novo")
    void tokenResolvesToClientId() {
        String token = authService.createToken(1L);
        String otherToken = authService.createToken(1L);

        assertThat(token).isNotEqualTo(otherToken);
        assertThat(authService.getClientIdFromToken(token)).isEqualTo(1L);
        assertThat(authService.getClientIdFromToken(otherToken)).isEqualTo(1L);
    }

    @Test
    @DisplayName("token desconhecido não resolve para nenhum cliente")
    void unknownToken() {
        assertThat(authService.getClientIdFromToken("token-inexistente")).isNull();
    }
}
