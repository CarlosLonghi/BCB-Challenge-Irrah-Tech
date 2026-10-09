package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.controller.request.AuthRequest;
import com.carloslonghi.bcb.controller.response.AuthResponse;
import com.carloslonghi.bcb.controller.response.ClientResponse;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.exception.DocumentNotRegisteredException;
import com.carloslonghi.bcb.mapper.ClientMapper;
import com.carloslonghi.bcb.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;
    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private AuthController controller;

    @Test
    @DisplayName("autentica o documento e devolve o token com os dados do cliente")
    void authenticate() {
        Client client = prePaidClient(1L, "10.00");
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(authService.authenticate("12345678901")).thenReturn(client);
        when(authService.createToken(1L)).thenReturn("token-123");
        when(clientMapper.toResponse(client)).thenReturn(clientResponse);

        ResponseEntity<AuthResponse> response = controller.authenticate(new AuthRequest("12345678901"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new AuthResponse("token-123", clientResponse));
    }

    @Test
    @DisplayName("documento não cadastrado: o erro propaga e nenhum token é criado")
    void unknownDocument() {
        when(authService.authenticate("00000000000")).thenThrow(new DocumentNotRegisteredException());

        assertThatThrownBy(() -> controller.authenticate(new AuthRequest("00000000000")))
                .isInstanceOf(DocumentNotRegisteredException.class);
        verify(authService, never()).createToken(any());
        verifyNoInteractions(clientMapper);
    }
}
