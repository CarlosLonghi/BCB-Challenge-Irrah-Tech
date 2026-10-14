package com.carloslonghi.bcb.entity;

import com.carloslonghi.bcb.entity.enums.LedgerEntryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tb_ledger_entries")
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LedgerEntryType type;

    // Sempre positivo; o tipo diz se saiu (DEBIT) ou voltou (REFUND)
    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
