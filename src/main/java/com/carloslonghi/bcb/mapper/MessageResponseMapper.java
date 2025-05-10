package com.carloslonghi.bcb.mapper;

import com.carloslonghi.bcb.dto.MessageResponseDTO;
import com.carloslonghi.bcb.model.Message;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneOffset;

@Component
public class MessageResponseMapper {
    public MessageResponseDTO toDTO(Message message, BigDecimal currentBalance) {
        return new MessageResponseDTO(
                message.getId(),
                message.getStatus(),
                message.getCreatedAt().plusSeconds(30).toInstant(ZoneOffset.UTC),
                message.getCost(),
                currentBalance
        );
    }
}
