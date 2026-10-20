package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.controller.api.spec.EventApi;
import com.carloslonghi.bcb.infra.events.MessageEventStream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * GET /events: stream SSE com as mudancas de status das mensagens do cliente logado.
 * Conexoes e envio dos eventos no MessageEventStream; documentacao em EventApi.
 */
@RestController
@RequiredArgsConstructor
public class EventController implements EventApi {

    private final MessageEventStream messageEventStream;

    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        Long clientId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return messageEventStream.subscribe(clientId);
    }
}
