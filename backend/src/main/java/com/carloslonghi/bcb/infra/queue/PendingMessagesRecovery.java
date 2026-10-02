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
        for (Message message : pending) {
            messageRepository.updateStatus(message.getId(), MessageStatus.QUEUED);
            messageQueue.enqueue(message);
        }

        if (!pending.isEmpty()) {
            log.info("{} mensagem(ns) pendente(s) reenfileirada(s) na inicializacao", pending.size());
        }
    }
}
