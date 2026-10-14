package com.carloslonghi.bcb.infra.worker;

import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.infra.events.MessageStatusChanged;
import com.carloslonghi.bcb.repository.MessageRef;
import com.carloslonghi.bcb.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Simula o destinatario lendo a mensagem: DELIVERED vira READ algum tempo depois da entrega.
 * Fica fora do worker para nao prender o consumidor da fila e para alcancar tambem as mensagens
 * entregues antes de um restart. Sai quando houver troca real de mensagens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReadSimulator {

    private final MessageRepository messageRepository;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    // Lote por rodada; o que sobrar fica para a proxima
    static final int BATCH_SIZE = 500;

    @Value("${bcb.simulation.read-after:PT4S}")
    private Duration readAfter;

    @Scheduled(initialDelayString = "${bcb.simulation.read-interval:PT2S}",
            fixedDelayString = "${bcb.simulation.read-interval:PT2S}")
    public void markDeliveredAsRead() {
        List<MessageRef> delivered =
                messageRepository.findDeliveredBefore(clock.instant().minus(readAfter), Limit.of(BATCH_SIZE));
        if (delivered.isEmpty()) {
            return;
        }

        // So esta rotina tira mensagens de DELIVERED, entao todas as selecionadas viram READ
        messageRepository.markRead(delivered.stream().map(MessageRef::id).toList());
        delivered.forEach(ref -> events.publishEvent(
                new MessageStatusChanged(ref.clientId(), ref.conversationId(), ref.id(), MessageStatus.READ)));
        log.debug("{} mensagem(ns) marcada(s) como lida(s)", delivered.size());
    }
}
