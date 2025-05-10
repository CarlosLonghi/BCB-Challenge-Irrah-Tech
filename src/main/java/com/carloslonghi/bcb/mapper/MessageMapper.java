package com.carloslonghi.bcb.mapper;

import com.carloslonghi.bcb.dto.MessageDTO;
import com.carloslonghi.bcb.model.Client;
import com.carloslonghi.bcb.model.Conversation;
import com.carloslonghi.bcb.model.Message;
import org.springframework.stereotype.Component;

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
        return new MessageDTO(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getRecipientId(),
                message.getContent(),
                message.getCreatedAt(),
                message.getPriority(),
                message.getStatus(),
                message.getCost()
        );
    }
}
