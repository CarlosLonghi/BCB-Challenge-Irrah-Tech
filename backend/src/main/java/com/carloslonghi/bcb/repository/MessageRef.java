package com.carloslonghi.bcb.repository;

// Ids de uma mensagem e de quem precisa saber dela, sem carregar a entidade
public record MessageRef(Long id, Long clientId, Long conversationId) {
}
