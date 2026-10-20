package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.controller.request.MessageRequest;
import com.carloslonghi.bcb.exception.MissingRecipientException;
import com.carloslonghi.bcb.exception.ReferencedEntityNotFoundException;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.entity.Conversation;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.repository.ConversationRepository;
import com.carloslonghi.bcb.repository.MessageRepository;
import com.carloslonghi.bcb.utils.PageLimit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Conversas do cliente logado com seus destinatarios e as mensagens de cada uma.
 * Toda busca filtra pelo principal: conversa de outro cliente vira 404.
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    private static final Sort MOST_RECENT_FIRST = Sort.by(Sort.Order.desc("lastMessageTime"), Sort.Order.desc("id"));

    // Paginada por page/size: a ordem por lastMessageTime muda a cada envio, entao nao serve de cursor
    public List<Conversation> findAll(int page, int size) {
        Long authId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return conversationRepository.findByClientId(authId,
                PageRequest.of(Math.max(page, 0), PageLimit.clamp(size), MOST_RECENT_FIRST));
    }

    // Conversa de outro cliente conta como inexistente
    public Conversation findById(Long id) {
        Long authId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return conversationRepository.findById(id)
                .filter(conv -> conv.getClient().getId().equals(authId))
                .orElseThrow(() -> new ReferencedEntityNotFoundException("Conversa", id));
    }

    // Usa a conversa informada ou cria uma nova com o destinatario do request. Roda dentro da transacao do
    // MessageService.sendMessage, que depois sobrescreve o resumo (lastMessage*) com os dados da mensagem gravada
    public Conversation findOrCreate(Client client, MessageRequest request) {
        if (request.conversationId() != null) {
            return findById(request.conversationId());
        }

        if (request.recipientId() == null || request.recipientName() == null || request.recipientName().isBlank()) {
            throw new MissingRecipientException();
        }

        Conversation conversation = new Conversation();
        conversation.setClient(client);
        conversation.setRecipientId(request.recipientId());
        conversation.setRecipientName(request.recipientName());
        conversation.setLastMessageContent(request.content());
        conversation.setLastMessageTime(Instant.now());
        conversation.setUnreadCount(0);
        return conversationRepository.save(conversation);
    }

    // As "limit" mais recentes antes do cursor, devolvidas em ordem crescente (como o chat mostra)
    public List<Message> findMessages(Long conversationId, Long before, int limit) {
        Conversation conversation = findById(conversationId);
        List<Message> newestFirst = before == null
                ? messageRepository.findByConversationIdOrderByIdDesc(conversation.getId(), PageLimit.of(limit))
                : messageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(conversation.getId(), before, PageLimit.of(limit));
        return newestFirst.reversed();
    }
}
