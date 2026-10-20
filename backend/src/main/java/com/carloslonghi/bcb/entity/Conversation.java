package com.carloslonghi.bcb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Conversa de um cliente com um destinatario (tb_conversations).
 * Guarda o resumo da ultima mensagem para a lista da barra lateral, atualizado a cada envio (MessageService.sendMessage).
 */
@Entity
@Table(name = "tb_conversations")
@Getter
@Setter
@NoArgsConstructor
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "last_message_content", nullable = false)
    private String lastMessageContent;

    @Column(name = "last_message_time", nullable = false)
    private Instant lastMessageTime;

    // Sempre 0 por enquanto: o sistema so envia; passa a valer quando houver recebimento de mensagens
    @Column(name = "unread_count", nullable = false)
    private int unreadCount;
}
