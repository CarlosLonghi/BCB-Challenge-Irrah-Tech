package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.controller.request.ClientRequest;
import com.carloslonghi.bcb.controller.request.ClientUpdateRequest;
import com.carloslonghi.bcb.controller.response.ClientBalanceResponse;
import com.carloslonghi.bcb.controller.response.ClientResponse;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.entity.enums.ClientDocumentType;
import com.carloslonghi.bcb.entity.enums.ClientPlanType;
import com.carloslonghi.bcb.mapper.ClientMapper;
import com.carloslonghi.bcb.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    @Mock
    private ClientService clientService;
    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientController controller;

    private Client client;
    private ClientResponse clientResponse;

    @BeforeEach
    void setUp() {
        client = prePaidClient(1L, "10.00");
        clientResponse = new ClientResponse(1L, "Cliente 1", "12345678901", ClientDocumentType.CPF,
                ClientPlanType.PRE_PAID, new BigDecimal("10.00"), BigDecimal.ZERO, true);
    }

    @Test
    @DisplayName("create devolve 201 com o cliente cadastrado")
    void create() {
        ClientRequest request = new ClientRequest("Cliente 1", "12345678901", ClientDocumentType.CPF,
                ClientPlanType.PRE_PAID, new BigDecimal("10.00"), BigDecimal.ZERO, true);
        when(clientMapper.toEntity(request)).thenReturn(client);
        when(clientService.create(client)).thenReturn(client);
        when(clientMapper.toResponse(client)).thenReturn(clientResponse);

        ResponseEntity<ClientResponse> response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(clientResponse);
    }

    @Test
    @DisplayName("findAll devolve o cliente autenticado")
    void findAll() {
        when(clientService.findAuthenticated()).thenReturn(List.of(client));
        when(clientMapper.toResponse(client)).thenReturn(clientResponse);

        ResponseEntity<List<ClientResponse>> response = controller.findAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(clientResponse);
    }

    @Test
    @DisplayName("findById devolve 200 com o cliente")
    void findById() {
        when(clientService.findById(1L)).thenReturn(client);
        when(clientMapper.toResponse(client)).thenReturn(clientResponse);

        ResponseEntity<ClientResponse> response = controller.findById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(clientResponse);
    }

    @Test
    @DisplayName("updateById repassa só o nome para o service")
    void updateById() {
        when(clientService.updateName(1L, "Novo Nome")).thenReturn(client);
        when(clientMapper.toResponse(client)).thenReturn(clientResponse);

        ResponseEntity<ClientResponse> response = controller.updateById(1L, new ClientUpdateRequest("Novo Nome"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(clientResponse);
    }

    @Test
    @DisplayName("getClientBalance devolve saldo e limite do cliente")
    void getClientBalance() {
        ClientBalanceResponse balance = new ClientBalanceResponse(new BigDecimal("10.00"), BigDecimal.ZERO);
        when(clientService.findById(1L)).thenReturn(client);
        when(clientMapper.toBalanceResponse(client)).thenReturn(balance);

        ResponseEntity<ClientBalanceResponse> response = controller.getClientBalance(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(balance);
    }
}
