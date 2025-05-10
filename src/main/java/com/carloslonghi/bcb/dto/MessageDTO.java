package com.carloslonghi.bcb.dto;

import com.carloslonghi.bcb.model.enums.MessagePriority;
import com.carloslonghi.bcb.model.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MessageDTO {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private int recipientId;
    private String content;
    private LocalDateTime createdAt;
    private MessagePriority priority;
    private MessageStatus status;
    private BigDecimal cost;
}
