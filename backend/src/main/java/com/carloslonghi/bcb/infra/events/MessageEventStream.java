package com.carloslonghi.bcb.infra.events;

import com.carloslonghi.bcb.controller.response.MessageStatusEventResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conexoes SSE abertas (GET /events), por cliente. Repassa cada MessageStatusChanged ao dono da mensagem,
 * em todas as abas dele. Fica em memoria: basta com uma instancia do backend.
 */
@Slf4j
@Component
public class MessageEventStream {

    public static final String STATUS_EVENT = "message-status";
    public static final String READY_EVENT = "ready";

    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // Ao vencer, o navegador reconecta; um token expirado entao recebe 401 e a sessao acaba
    @Value("${bcb.events.timeout:PT30M}")
    private Duration timeout;

    public SseEmitter subscribe(Long clientId) {
        SseEmitter emitter = new SseEmitter(timeout.toMillis());
        emitters.computeIfAbsent(clientId, id -> ConcurrentHashMap.newKeySet()).add(emitter);

        Runnable remove = () -> remove(clientId, emitter);
        emitter.onCompletion(remove);
        // complete() fecha a resposta normalmente; sem ele o Spring loga um AsyncRequestTimeoutException
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> remove.run());

        // Primeiro evento: manda os headers na hora e avisa o frontend que a conexao esta de pe
        send(clientId, emitter, SseEmitter.event().name(READY_EVENT).data(""));
        return emitter;
    }

    // AFTER_COMMIT: o FAILED so sai depois do commit do estorno. Fora de transacao (worker), envia na hora
    @TransactionalEventListener(fallbackExecution = true)
    public void onStatusChanged(MessageStatusChanged event) {
        Set<SseEmitter> clientEmitters = emitters.get(event.clientId());
        if (clientEmitters == null) {
            return;
        }
        MessageStatusEventResponse data =
                new MessageStatusEventResponse(event.messageId(), event.conversationId(), event.status());
        for (SseEmitter emitter : clientEmitters) {
            send(event.clientId(), emitter, SseEmitter.event().name(STATUS_EVENT).data(data, MediaType.APPLICATION_JSON));
        }
    }

    // Comentario periodico: mantem a conexao viva em proxies e descobre as que cairam sem aviso
    @Scheduled(initialDelayString = "${bcb.events.heartbeat:PT20S}", fixedDelayString = "${bcb.events.heartbeat:PT20S}")
    public void heartbeat() {
        emitters.forEach((clientId, clientEmitters) ->
                clientEmitters.forEach(emitter -> send(clientId, emitter, SseEmitter.event().comment("ping"))));
    }

    int connections(Long clientId) {
        return emitters.getOrDefault(clientId, Set.of()).size();
    }

    private void send(Long clientId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            // Aba fechada ou emitter ja encerrado: so tira da lista
            log.debug("Conexao SSE do cliente {} encerrada: {}", clientId, e.getMessage());
            remove(clientId, emitter);
        }
    }

    private void remove(Long clientId, SseEmitter emitter) {
        emitters.computeIfPresent(clientId, (id, clientEmitters) -> {
            clientEmitters.remove(emitter);
            return clientEmitters.isEmpty() ? null : clientEmitters;
        });
    }
}
