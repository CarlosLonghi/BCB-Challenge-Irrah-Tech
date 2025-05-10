package com.carloslonghi.bcb.mapper;

import com.carloslonghi.bcb.dto.MessageRequestDTO;
import com.carloslonghi.bcb.model.Client;
import com.carloslonghi.bcb.model.Conversation;
import com.carloslonghi.bcb.model.Message;
import com.carloslonghi.bcb.model.enums.MessageStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class MessageRequestMapper {
    public Message toModel(MessageRequestDTO dto,
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
}
