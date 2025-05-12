package com.carloslonghi.bcb.model;

import com.carloslonghi.bcb.model.enums.ClientDocumentType;
import com.carloslonghi.bcb.model.enums.ClientPlanType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_clients")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String document;

    @Column(name = "document_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ClientDocumentType documentType;

    @Column(name = "plan_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ClientPlanType planType;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(name = "client_limit", nullable = false)
    private BigDecimal limit;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
