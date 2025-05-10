package com.carloslonghi.bcb.mapper;

import com.carloslonghi.bcb.dto.MessageDTO;
import com.carloslonghi.bcb.dto.MessageRequestDTO;
import com.carloslonghi.bcb.dto.MessageResponseDTO;
import com.carloslonghi.bcb.model.Client;
import com.carloslonghi.bcb.model.Conversation;
import com.carloslonghi.bcb.model.Message;
import com.carloslonghi.bcb.model.enums.MessageStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class MessageMapper {
    public Message toModel(MessageDTO dto, Conversation conversation, Client sender) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setRecipientId(dto.getRecipientId());
        message.setContent(dto.getContent());
        message.setCreatedAt(dto.getCreatedAt());
        message.setPriority(dto.getPriority());
        message.setStatus(dto.getStatus());
        message.setCost(dto.getCost());

        return message;
    }

    public MessageDTO toDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setRecipientId(message.getRecipientId());
        dto.setContent(message.getContent());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setPriority(message.getPriority());
        dto.setStatus(message.getStatus());
        dto.setCost(message.getCost());

        return dto;
    }

    public Message toRequestModel(MessageRequestDTO dto,
                           Client sender,
                           Conversation conversation,
                           BigDecimal cost,
                           LocalDateTime createdAt) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setRecipientId(dto.getRecipientId());
        message.setContent(dto.getContent());
        message.setPriority(dto.getPriority());
        message.setStatus(MessageStatus.QUEUED);
        message.setCost(cost);
        message.setCreatedAt(createdAt);

        return message;
    }

    public MessageResponseDTO toResponseDTO(Message message, BigDecimal currentBalance) {
        MessageResponseDTO dto = new MessageResponseDTO();
        dto.setId(message.getId());
        dto.setStatus(message.getStatus());
        dto.setEstimatedDelivery(message.getCreatedAt().plusSeconds(30).toInstant(ZoneOffset.UTC));
        dto.setCost(message.getCost());
        dto.setCurrentBalance(currentBalance);

        return dto;
    }
}
