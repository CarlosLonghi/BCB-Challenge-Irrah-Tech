package com.carloslonghi.bcb.entity;

import com.carloslonghi.bcb.entity.enums.ClientDocumentType;
import com.carloslonghi.bcb.entity.enums.ClientPlanType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Empresa que envia mensagens pagas (tb_clients).
 * Pre-pago gasta de {@code balance}; pos-pago gasta de {@code limit}, que guarda o limite restante, e nao o total.
 * Os dois so mudam por ClientService.debit/refund, com o cliente travado.
 * Mudou um campo: migration nova (ver CLAUDE.md).
 */
@Entity
@Table(name = "tb_clients")
@Getter
@Setter
@NoArgsConstructor
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

    // Limite restante do pos-pago. Coluna client_limit porque "limit" e palavra reservada no SQL
    @Column(name = "client_limit", nullable = false)
    private BigDecimal limit;

    @Column(nullable = false)
    private boolean active;

    // Hash BCrypt; nulo para clientes cadastrados antes da senha existir (esses nao conseguem entrar)
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
