package com.carloslonghi.bcb.dto;

import com.carloslonghi.bcb.model.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MessageResponseDTO {
    private Long id;
    private MessageStatus status;
    private Instant estimatedDelivery;
    private BigDecimal cost;
    private BigDecimal currentBalance;  // so para pre-pago
}
