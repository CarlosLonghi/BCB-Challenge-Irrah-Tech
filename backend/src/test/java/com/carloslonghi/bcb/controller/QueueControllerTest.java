package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.controller.response.QueueStatusResponse;
import com.carloslonghi.bcb.service.QueueStatusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueControllerTest {

    @Mock
    private QueueStatusService queueStatusService;

    @InjectMocks
    private QueueController controller;

    @Test
    @DisplayName("getStatus devolve o status da fila")
    void getStatus() {
        QueueStatusResponse status = new QueueStatusResponse(3, 1, 2, 0, 0, 5, 0);
        when(queueStatusService.getStatus()).thenReturn(status);

        ResponseEntity<QueueStatusResponse> response = controller.getStatus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(status);
    }
}
