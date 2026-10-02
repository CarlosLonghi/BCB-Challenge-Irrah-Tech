package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.controller.request.MessageRequest;
import com.carloslonghi.bcb.exception.ReferencedEntityNotFoundException;
import com.carloslonghi.bcb.infra.queue.MessageQueue;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.entity.Conversation;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.repository.ClientRepository;
import com.carloslonghi.bcb.repository.ConversationRepository;
import com.carloslonghi.bcb.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ClientRepository clientRepository;

    private final ClientService clientService;
    private final ConversationService conversationService;

    private final MessageQueue messageQueue;

    private static final BigDecimal NORMAL_COST = new BigDecimal("0.25");
    private static final BigDecimal URGENT_COST = new BigDecimal("0.50");

    // Debito, mensagem e conversa sao gravados juntos: se algo falhar, nada e cobrado
    @Transactional
    public Message sendMessage(MessageRequest request) {
        Long senderId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Client sender = clientRepository.findWithLockById(senderId)
                .orElseThrow(() -> new ReferencedEntityNotFoundException("Cliente", senderId));

        Conversation conversation = conversationService.findOrCreate(sender, request);

        BigDecimal cost = request.priority() == MessagePriority.URGENT ? URGENT_COST : NORMAL_COST;
        clientService.debit(sender, cost);

        Message message = messageRepository.save(newMessage(sender, conversation, request, cost));

        conversation.setLastMessageContent(message.getContent());
        conversation.setLastMessageTime(message.getCreatedAt());
        conversationRepository.save(conversation);

        enqueueAfterCommit(message);
        return message;
    }

    public List<Message> findAll() {
        Long authId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return messageRepository.findBySenderId(authId);
    }

    public Message findById(Long id) {
        Long authId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return messageRepository.findById(id)
                .filter(msg -> msg.getSender().getId().equals(authId))
                .orElseThrow(() -> new ReferencedEntityNotFoundException("Mensagem", id));
    }

    public MessageStatus getStatus(Long id) {
        return findById(id).getStatus();
    }

    private Message newMessage(Client sender, Conversation conversation, MessageRequest request, BigDecimal cost) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setRecipientId(conversation.getRecipientId());
        message.setContent(request.content());
        message.setPriority(request.priority());
        message.setStatus(MessageStatus.QUEUED);
        message.setCost(cost);
        message.setCreatedAt(Instant.now());
        return message;
    }

    // So enfileira depois do commit, senao o worker poderia pegar uma mensagem que ainda nao existe no banco
    private void enqueueAfterCommit(Message message) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // O debito ja foi gravado; se a fila estiver fora do ar a mensagem fica QUEUED
                // no banco e o PendingMessagesRecovery a reenfileira na proxima inicializacao
                try {
                    messageQueue.enqueue(message);
                } catch (Exception e) {
                    log.error("Nao foi possivel enfileirar a mensagem {}", message.getId(), e);
                }
            }
        });
    }
}
