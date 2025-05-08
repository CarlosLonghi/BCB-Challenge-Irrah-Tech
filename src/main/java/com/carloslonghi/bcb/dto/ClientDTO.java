package com.carloslonghi.bcb.dto;

import com.carloslonghi.bcb.model.enums.ClientDocumentType;
import com.carloslonghi.bcb.model.enums.ClientPlanType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ClientDTO {
    private Long id;
    private String name;
    private String document;
    private ClientDocumentType documentType;
    private ClientPlanType planType;
    private BigDecimal balance;
    private BigDecimal limit;
    private boolean active;
}
