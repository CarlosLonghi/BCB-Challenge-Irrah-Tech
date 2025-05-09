package com.carloslonghi.bcb.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_conversations")
@NoArgsConstructor
@AllArgsConstructor
@Data
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
    private LocalDateTime lastMessageTime;

    @Column(name = "unread_count", nullable = false)
    private int unreadCount;
}
