package com.carloslonghi.bcb.infra.events;

import com.carloslonghi.bcb.entity.enums.MessageStatus;

/**
 * Status de uma mensagem mudou. Publicado com o ApplicationEventPublisher por quem muda o status
 * (worker, leitura simulada, estorno); o MessageEventStream repassa ao cliente dono da mensagem.
 */
public record MessageStatusChanged(Long clientId, Long conversationId, Long messageId, MessageStatus status) {
}
