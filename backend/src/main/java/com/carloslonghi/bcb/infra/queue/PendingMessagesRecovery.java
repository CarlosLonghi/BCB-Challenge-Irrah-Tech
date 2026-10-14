package com.carloslonghi.bcb.infra.queue;

import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

// Devolve para a fila as mensagens que ficaram pelo caminho:
// - na inicializacao, as que estavam QUEUED, PROCESSING ou SENT quando o backend parou. SENT entra tambem:
//   sem isso a mensagem ficaria parada e o frontend faria polling dela para sempre;
// - periodicamente, as QUEUED que nunca chegaram ao RabbitMQ (publicacao falhou no afterCommit).
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingMessagesRecovery {

    private static final List<MessageStatus> PENDING_STATUSES =
            List.of(MessageStatus.QUEUED, MessageStatus.PROCESSING, MessageStatus.SENT);

    private final MessageRepository messageRepository;
    private final MessageQueue messageQueue;
    private final Clock clock;

    // Folga antes de considerar a publicacao perdida: o afterCommit pode estar publicando agora
    @Value("${bcb.recovery.unpublished-grace:PT10S}")
    private Duration unpublishedGrace;

    @EventListener(ApplicationReadyEvent.class)
    public void requeuePendingMessages() {
        List<Message> pending = messageRepository.findByStatusInOrderByCreatedAtAsc(PENDING_STATUSES);

        // Volta para QUEUED: a mensagem vai esperar na fila de novo, e o frontend nao deve mostrar "processando" enquanto isso
        int requeued = 0;
        for (Message message : pending) {
            // Falha numa mensagem (ex.: RabbitMQ fora do ar) nao derruba a inicializacao nem impede as proximas;
            // ela fica QUEUED sem enqueued_at e a rotina periodica tenta de novo
            try {
                messageRepository.resetToUnpublished(message.getId());
                publish(message);
                requeued++;
            } catch (Exception e) {
                log.error("Nao foi possivel reenfileirar a mensagem {}", message.getId(), e);
            }
        }

        if (!pending.isEmpty()) {
            log.info("{} de {} mensagem(ns) pendente(s) reenfileirada(s) na inicializacao", requeued, pending.size());
        }
    }

    @Scheduled(initialDelayString = "${bcb.recovery.unpublished-interval:PT30S}",
            fixedDelayString = "${bcb.recovery.unpublished-interval:PT30S}")
    public void requeueUnpublished() {
        Instant cutoff = clock.instant().minus(unpublishedGrace);
        List<Message> unpublished = messageRepository
                .findByStatusAndEnqueuedAtIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(MessageStatus.QUEUED, cutoff);

        int requeued = 0;
        for (Message message : unpublished) {
            try {
                publish(message);
                requeued++;
            } catch (Exception e) {
                log.error("Nao foi possivel publicar a mensagem {}; nova tentativa na proxima rodada", message.getId(), e);
            }
        }

        if (!unpublished.isEmpty()) {
            log.info("{} de {} mensagem(ns) sem publicacao reenfileirada(s)", requeued, unpublished.size());
        }
    }

    private void publish(Message message) {
        messageQueue.enqueue(message);
        messageRepository.markEnqueued(message.getId(), clock.instant());
    }
}
