package com.carloslonghi.bcb.infra.worker;

import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.infra.events.MessageStatusChanged;
import com.carloslonghi.bcb.infra.metrics.MessageMetrics;
import com.carloslonghi.bcb.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Simula o envio de uma mensagem: PROCESSING -> SENT -> DELIVERED (o READ vem depois, pelo ReadSimulator).
 * Cada etapa e gravada no banco e publicada como MessageStatusChanged (o frontend recebe por SSE).
 * Chamado pelo RabbitMessageListener.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageProcessor {

    private static final Set<MessageStatus> FINAL_STATUSES =
            Set.of(MessageStatus.DELIVERED, MessageStatus.READ, MessageStatus.FAILED);

    private final MessageRepository messageRepository;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private final MessageMetrics metrics;

    // Pausa entre as etapas; os testes zeram para nao esperar
    @Value("${bcb.worker.step-delay-ms:4000}")
    private long stepDelayMs;

    public void process(Long messageId) throws InterruptedException {
        Message message = messageRepository.findById(messageId).orElse(null);

        // Mensagem apagada ou ja finalizada (ex.: entregue duas vezes pela fila): nada a fazer
        if (message == null || FINAL_STATUSES.contains(message.getStatus())) {
            return;
        }

        messageRepository.updateStatus(messageId, MessageStatus.PROCESSING);
        publish(message, MessageStatus.PROCESSING);
        Thread.sleep(stepDelayMs);

        messageRepository.updateStatus(messageId, MessageStatus.SENT);
        publish(message, MessageStatus.SENT);
        Thread.sleep(stepDelayMs);

        Instant deliveredAt = clock.instant();
        messageRepository.markDelivered(messageId, deliveredAt);
        publish(message, MessageStatus.DELIVERED);
        Duration sinceCreation = Duration.between(message.getCreatedAt(), deliveredAt);
        metrics.delivered(message.getPriority(), sinceCreation);
        // Mesmo trace.id do log "enfileirada" da requisicao: o traceId vem nos headers da mensagem do RabbitMQ
        log.info("Mensagem {} entregue {} ms depois de criada", messageId, sinceCreation.toMillis());
    }

    // getId() das associacoes LAZY nao vai ao banco: o proxy ja conhece o id
    private void publish(Message message, MessageStatus status) {
        events.publishEvent(new MessageStatusChanged(
                message.getSender().getId(), message.getConversation().getId(), message.getId(), status));
    }
}
