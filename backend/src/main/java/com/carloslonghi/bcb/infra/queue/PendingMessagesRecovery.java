package com.carloslonghi.bcb.infra.queue;

import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

// Na inicializacao, reenfileira as mensagens que ficaram pela metade (QUEUED, PROCESSING ou SENT) quando o backend parou.
// SENT entra tambem: sem isso a mensagem ficaria parada e o frontend faria polling dela para sempre
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingMessagesRecovery {

    private static final List<MessageStatus> PENDING_STATUSES =
            List.of(MessageStatus.QUEUED, MessageStatus.PROCESSING, MessageStatus.SENT);

    private final MessageRepository messageRepository;
    private final MessageQueue messageQueue;

    @EventListener(ApplicationReadyEvent.class)
    public void requeuePendingMessages() {
        List<Message> pending = messageRepository.findByStatusInOrderByCreatedAtAsc(PENDING_STATUSES);

        // Volta para QUEUED: a mensagem vai esperar na fila de novo, e o frontend nao deve mostrar "processando" enquanto isso
        int requeued = 0;
        for (Message message : pending) {
            // Falha numa mensagem (ex.: RabbitMQ fora do ar) nao derruba a inicializacao nem impede as proximas;
            // ela fica no banco e volta a ser tentada na proxima inicializacao
            try {
                messageRepository.updateStatus(message.getId(), MessageStatus.QUEUED);
                messageQueue.enqueue(message);
                requeued++;
            } catch (Exception e) {
                log.error("Nao foi possivel reenfileirar a mensagem {}", message.getId(), e);
            }
        }

        if (!pending.isEmpty()) {
            log.info("{} de {} mensagem(ns) pendente(s) reenfileirada(s) na inicializacao", requeued, pending.size());
        }
    }
}
