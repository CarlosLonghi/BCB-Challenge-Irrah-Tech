package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.infra.events.MessageEventStream;
import com.carloslonghi.bcb.support.AuthenticatedClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private MessageEventStream messageEventStream;

    @InjectMocks
    private EventController controller;

    @AfterEach
    void tearDown() {
        AuthenticatedClient.logout();
    }

    @Test
    @DisplayName("subscribe abre o stream do cliente logado")
    void subscribe() {
        AuthenticatedClient.login(1L);
        SseEmitter emitter = new SseEmitter();
        when(messageEventStream.subscribe(1L)).thenReturn(emitter);

        assertThat(controller.subscribe()).isSameAs(emitter);
    }
}
