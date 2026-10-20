package com.carloslonghi.bcb.repository;

/**
 * Ids de uma mensagem e de quem precisa saber dela (dono e conversa), sem carregar a entidade.
 * Usado pelo ReadSimulator para publicar o MessageStatusChanged.
 */
public record MessageRef(Long id, Long clientId, Long conversationId) {
}
