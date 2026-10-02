package com.carloslonghi.bcb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tb_conversations")
@Getter
@Setter
@NoArgsConstructor
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "recipient_id", nullable = false)
    private int recipientId;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "last_message_content", nullable = false)
    private String lastMessageContent;

    @Column(name = "last_message_time", nullable = false)
    private Instant lastMessageTime;

    @Column(name = "unread_count", nullable = false)
    private int unreadCount;
}
