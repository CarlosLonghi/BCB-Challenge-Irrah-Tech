package com.carloslonghi.bcb.entity;

import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Mensagem paga enviada por um cliente (tb_messages). Ciclo de status em MessageStatus.
 * <p>
 * O status muda so pelos updates de coluna do MessageRepository (updateStatus, markDelivered...), nunca salvando
 * a entidade inteira: o worker e as rotinas agendadas trabalham em paralelo, e um save() sobrescreveria a linha
 * com uma copia antiga.
 */
@Entity
@Table(name = "tb_messages")
@Getter
@Setter
@NoArgsConstructor
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private Client sender;

    // Copia do destinatario da conversa no momento do envio
    @Column(name = "recipient_id")
    private Long recipientId;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MessagePriority priority;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageStatus status;

    // Valor cobrado no envio; e o que o estorno devolve
    @Column(nullable = false)
    private BigDecimal cost;

    // Quando foi publicada no RabbitMQ; nulo enquanto a publicacao nao deu certo
    @Column(name = "enqueued_at")
    private Instant enqueuedAt;

    // Quando chegou a DELIVERED; o ReadSimulator usa para marcar READ depois
    @Column(name = "delivered_at")
    private Instant deliveredAt;
}
