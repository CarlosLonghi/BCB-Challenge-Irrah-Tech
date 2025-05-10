package com.carloslonghi.bcb.mapper;

import com.carloslonghi.bcb.dto.ConversationDTO;
import com.carloslonghi.bcb.model.Client;
import com.carloslonghi.bcb.model.Conversation;
import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {
    public Conversation toModel(ConversationDTO dto, Client client) {
        Conversation conversation = new Conversation();
        conversation.setId(dto.getId());
        conversation.setClient(client);
        conversation.setRecipientId(dto.getRecipientId());
        conversation.setRecipientName(dto.getRecipientName());
        conversation.setLastMessageContent(dto.getLastMessageContent());
        conversation.setLastMessageTime(dto.getLastMessageTime());
        conversation.setUnreadCount(dto.getUnreadCount());

        return conversation;
    }

    public ConversationDTO toDTO(Conversation conversation) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());
        dto.setClientId(conversation.getClient().getId());
        dto.setRecipientId(conversation.getRecipientId());
        dto.setRecipientName(conversation.getRecipientName());
        dto.setLastMessageContent(conversation.getLastMessageContent());
        dto.setLastMessageTime(conversation.getLastMessageTime());
        dto.setUnreadCount(conversation.getUnreadCount());

        return dto;
    }
}
