package com.carloslonghi.bcb.dto;

import com.carloslonghi.bcb.model.enums.MessagePriority;
import lombok.Data;

@Data
public class MessageRequestDTO {
    private Long conversationId; // necessario ao continuar conversas existentes
    private int recipientId;
    private String recipientName; // necessario ao criar conversas
    private String content;
    private MessagePriority priority;
}
