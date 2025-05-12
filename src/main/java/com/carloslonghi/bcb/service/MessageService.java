package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.dto.MessageDTO;
import com.carloslonghi.bcb.dto.MessageRequestDTO;
import com.carloslonghi.bcb.dto.MessageResponseDTO;
import com.carloslonghi.bcb.infra.queue.PriorityMessageQueue;
import com.carloslonghi.bcb.mapper.MessageMapper;
import com.carloslonghi.bcb.model.Client;
import com.carloslonghi.bcb.model.Conversation;
import com.carloslonghi.bcb.model.Message;
import com.carloslonghi.bcb.model.enums.ClientPlanType;
import com.carloslonghi.bcb.model.enums.MessagePriority;
import com.carloslonghi.bcb.model.enums.MessageStatus;
import com.carloslonghi.bcb.repository.ClientRepository;
import com.carloslonghi.bcb.repository.ConversationRepository;
import com.carloslonghi.bcb.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ClientRepository clientRepository;

    private final MessageMapper messageMapper;
    private final PriorityMessageQueue priorityMessageQueue;

    public MessageResponseDTO sendMessage(MessageRequestDTO dto) {
        Long senderId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Client sender = clientRepository.findById(senderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Remetente invalido"));

        Conversation conversation;

        if (dto.getConversationId() != null) {
            // Busca conversa existente
            conversation = conversationRepository.findById(dto.getConversationId())
                    .orElseThrow(() -> new RuntimeException("Conversa não encontrada"));
        } else {
            // Cria nova conversa
            Conversation newConversation = new Conversation();
            newConversation.setClient(sender);
            newConversation.setRecipientId(dto.getRecipientId());
            newConversation.setRecipientName(dto.getRecipientName());
            newConversation.setLastMessageContent(dto.getContent());
            newConversation.setLastMessageTime(LocalDateTime.now());
            newConversation.setUnreadCount(0);
            conversation = conversationRepository.save(newConversation);
        }

        BigDecimal messageCost = dto.getPriority() == MessagePriority.URGENT
                ? new BigDecimal("0.50")
                : new BigDecimal("0.25");

        BigDecimal currentBalance = null;

        // Debito de saldo caso pre-pago
        if (sender.getPlanType() == ClientPlanType.PRE_PAID) {
            if (sender.getBalance().compareTo(messageCost) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.PAYMENT_REQUIRED, "Saldo insuficiente");
            }
            currentBalance = sender.getBalance().subtract(messageCost);
            sender.setBalance(currentBalance);
            clientRepository.save(sender);
        }

        // Debito de saldo caso pos-pago
        if (sender.getPlanType() == ClientPlanType.POST_PAID) {
            BigDecimal remainingLimit = sender.getLimit().subtract(messageCost);
            if (remainingLimit.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.PAYMENT_REQUIRED, "Limite de consumo excedido");
            }
            sender.setLimit(remainingLimit);
            clientRepository.save(sender);
            currentBalance = remainingLimit;
        }

        // Cria mensagem
        LocalDateTime createdAt = LocalDateTime.now();
        Message message = messageMapper.toRequestModel(dto, sender, conversation, messageCost, createdAt);

        message = messageRepository.save(message);

        // Atualiza conversa
        conversation.setLastMessageContent(dto.getContent());
        conversation.setLastMessageTime(message.getCreatedAt());
        conversation.setUnreadCount(conversation.getUnreadCount() + 1);
        conversationRepository.save(conversation);

        priorityMessageQueue.enqueue(message);

        return messageMapper.toResponseDTO(message, currentBalance);
    }

    public List<MessageDTO> findAll() {
        Long authId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        List<Message> messages = messageRepository.findBySenderId(authId);

        return messages.stream()
                .map(messageMapper::toDTO)
                .collect(Collectors.toList());
    }

    public MessageDTO findById(Long id) {
        Long authId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Message message = messageRepository.findById(id)
                .filter(msg -> msg.getSender().getId().equals(authId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Mensagem com id " + id + " não encontrada"
                ));

        return messageMapper.toDTO(message);
    }

    public MessageStatus getStatus(Long id) {
        Long authId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Message message = messageRepository.findById(id)
                .filter(msg -> msg.getSender().getId().equals(authId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Mensagem com id " + id + " não encontrada"
                ));

        return message.getStatus();
    }
}
