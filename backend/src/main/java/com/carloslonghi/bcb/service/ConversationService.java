package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.controller.request.MessageRequest;
import com.carloslonghi.bcb.exception.MissingRecipientException;
import com.carloslonghi.bcb.exception.ReferencedEntityNotFoundException;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.entity.Conversation;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.repository.ConversationRepository;
import com.carloslonghi.bcb.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public List<Conversation> findAll() {
        Long authId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return conversationRepository.findByClientIdOrderByLastMessageTimeDesc(authId);
    }

    // Conversa de outro cliente conta como inexistente
    public Conversation findById(Long id) {
        Long authId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return conversationRepository.findById(id)
                .filter(conv -> conv.getClient().getId().equals(authId))
                .orElseThrow(() -> new ReferencedEntityNotFoundException("Conversa", id));
    }

    // Usa a conversa informada ou cria uma nova com o destinatario do request
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

    public List<Message> findMessages(Long conversationId) {
        Conversation conversation = findById(conversationId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
    }
}
