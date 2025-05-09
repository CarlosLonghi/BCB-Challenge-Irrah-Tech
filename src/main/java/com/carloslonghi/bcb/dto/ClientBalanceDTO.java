package com.carloslonghi.bcb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@AllArgsConstructor
@Data
public class ClientBalanceDTO {
    private BigDecimal balance;
    private BigDecimal limit;
}
