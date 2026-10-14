package com.carloslonghi.bcb.infra.worker;

import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Set;

/**
 * Simula o envio de uma mensagem: PROCESSING -> SENT -> DELIVERED (o READ vem depois, pelo ReadSimulator).
 * Cada etapa e gravada no banco, porque o frontend acompanha o status por polling.
 * Chamado pelo RabbitMessageListener.
 */
@Component
@RequiredArgsConstructor
public class MessageProcessor {

    private static final Set<MessageStatus> FINAL_STATUSES =
            Set.of(MessageStatus.DELIVERED, MessageStatus.READ, MessageStatus.FAILED);

    private final MessageRepository messageRepository;
    private final Clock clock;

    // Pausa entre as etapas; os testes zeram para nao esperar
    @Value("${bcb.worker.step-delay-ms:4000}")
    private long stepDelayMs;

    public void process(Long messageId) throws InterruptedException {
        MessageStatus current = messageRepository.findById(messageId)
                .map(message -> message.getStatus())
                .orElse(null);

        // Mensagem apagada ou ja finalizada (ex.: entregue duas vezes pela fila): nada a fazer
        if (current == null || FINAL_STATUSES.contains(current)) {
            return;
        }

        messageRepository.updateStatus(messageId, MessageStatus.PROCESSING);
        Thread.sleep(stepDelayMs);

        messageRepository.updateStatus(messageId, MessageStatus.SENT);
        Thread.sleep(stepDelayMs);

        messageRepository.markDelivered(messageId, clock.instant());
    }
}
