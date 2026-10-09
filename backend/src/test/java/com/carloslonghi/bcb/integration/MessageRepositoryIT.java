package com.carloslonghi.bcb.integration;

import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.entity.Conversation;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.repository.ClientRepository;
import com.carloslonghi.bcb.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.assertj.core.api.Assertions.assertThat;

// Queries do MessageRepository contra o Postgres real, incluindo o update com @Transactional + @Modifying
class MessageRepositoryIT extends IntegrationTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    private Conversation conversation;

    @BeforeEach
    void setUp() {
        Client client = prePaidClient(null, "10.00");
        clientRepository.save(client);

        Conversation newConversation = new Conversation();
        newConversation.setClient(client);
        newConversation.setRecipientId(55);
        newConversation.setRecipientName("Maria Souza");
        newConversation.setLastMessageContent("Oi");
        newConversation.setLastMessageTime(Instant.now());
        conversation = conversationRepository.save(newConversation);
    }

    private Message save(MessagePriority priority, MessageStatus status, Instant createdAt) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(conversation.getClient());
        message.setRecipientId(55);
        message.setContent("Olá");
        message.setPriority(priority);
        message.setStatus(status);
        message.setCost(new java.math.BigDecimal("0.25"));
        message.setCreatedAt(createdAt);
        return messageRepository.save(message);
    }

    @Test
    @DisplayName("updateStatus funciona chamado fora de transação e altera só o status")
    void updateStatusOutsideTransaction() {
        Message message = save(MessagePriority.NORMAL, MessageStatus.QUEUED, Instant.now());

        messageRepository.updateStatus(message.getId(), MessageStatus.SENT);

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM tb_messages WHERE id = ?", String.class, message.getId()))
                .isEqualTo("SENT");
        assertThat(jdbcTemplate.queryForObject("SELECT content FROM tb_messages WHERE id = ?", String.class, message.getId()))
                .isEqualTo("Olá");
    }

    @Test
    @DisplayName("contagens por status e prioridade")
    void counts() {
        save(MessagePriority.URGENT, MessageStatus.QUEUED, Instant.now());
        save(MessagePriority.NORMAL, MessageStatus.QUEUED, Instant.now());
        save(MessagePriority.NORMAL, MessageStatus.QUEUED, Instant.now());
        save(MessagePriority.NORMAL, MessageStatus.DELIVERED, Instant.now());

        assertThat(messageRepository.countByStatusAndPriority(MessageStatus.QUEUED, MessagePriority.URGENT)).isEqualTo(1);
        assertThat(messageRepository.countByStatusAndPriority(MessageStatus.QUEUED, MessagePriority.NORMAL)).isEqualTo(2);
        assertThat(messageRepository.countByStatus(MessageStatus.DELIVERED)).isEqualTo(1);
        assertThat(messageRepository.countByStatus(MessageStatus.FAILED)).isZero();
    }

    @Test
    @DisplayName("pendentes vêm filtradas pelos status pedidos e da mais antiga para a mais nova")
    void pendingOrderedByCreation() {
        Instant now = Instant.now();
        Message newest = save(MessagePriority.NORMAL, MessageStatus.QUEUED, now);
        save(MessagePriority.NORMAL, MessageStatus.DELIVERED, now.minusSeconds(30));
        Message oldest = save(MessagePriority.URGENT, MessageStatus.PROCESSING, now.minusSeconds(20));
        Message middle = save(MessagePriority.NORMAL, MessageStatus.SENT, now.minusSeconds(10));

        List<Message> pending = messageRepository.findByStatusInOrderByCreatedAtAsc(
                List.of(MessageStatus.QUEUED, MessageStatus.PROCESSING, MessageStatus.SENT));

        assertThat(pending).extracting(Message::getId)
                .containsExactly(oldest.getId(), middle.getId(), newest.getId());
    }

    @Test
    @DisplayName("mensagens da conversa em ordem cronológica")
    void conversationMessagesInOrder() {
        Instant now = Instant.now();
        Message second = save(MessagePriority.NORMAL, MessageStatus.SENT, now);
        Message first = save(MessagePriority.NORMAL, MessageStatus.SENT, now.minusSeconds(5));

        assertThat(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()))
                .extracting(Message::getId)
                .containsExactly(first.getId(), second.getId());
    }
}
