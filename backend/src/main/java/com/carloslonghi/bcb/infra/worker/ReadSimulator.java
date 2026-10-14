package com.carloslonghi.bcb.infra.worker;

import com.carloslonghi.bcb.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

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

    @Value("${bcb.simulation.read-after:PT4S}")
    private Duration readAfter;

    @Scheduled(initialDelayString = "${bcb.simulation.read-interval:PT2S}",
            fixedDelayString = "${bcb.simulation.read-interval:PT2S}")
    public void markDeliveredAsRead() {
        int read = messageRepository.markReadDeliveredBefore(clock.instant().minus(readAfter));
        if (read > 0) {
            log.debug("{} mensagem(ns) marcada(s) como lida(s)", read);
        }
    }
}
