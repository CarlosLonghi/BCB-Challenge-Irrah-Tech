package com.carloslonghi.bcb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ConversationDTO {
    private Long id;
    private Long clientId;
    private int recipientId;
    private String recipientName;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private int unreadCount;
}
