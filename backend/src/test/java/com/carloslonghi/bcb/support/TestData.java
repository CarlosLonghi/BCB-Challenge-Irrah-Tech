package com.carloslonghi.bcb.support;

import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.entity.Conversation;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.ClientDocumentType;
import com.carloslonghi.bcb.entity.enums.ClientPlanType;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;

import java.math.BigDecimal;
import java.time.Instant;

public final class TestData {

    private TestData() {
    }

    public static Client prePaidClient(Long id, String balance) {
        return client(id, ClientPlanType.PRE_PAID, new BigDecimal(balance), BigDecimal.ZERO);
    }

    public static Client postPaidClient(Long id, String limit) {
        return client(id, ClientPlanType.POST_PAID, BigDecimal.ZERO, new BigDecimal(limit));
    }

    public static Client client(Long id, ClientPlanType planType, BigDecimal balance, BigDecimal limit) {
        Client client = new Client();
        client.setId(id);
        client.setName("Cliente " + id);
        client.setDocument("12345678901");
        client.setDocumentType(ClientDocumentType.CPF);
        client.setPlanType(planType);
        client.setBalance(balance);
        client.setLimit(limit);
        client.setActive(true);
        return client;
    }

    public static Conversation conversation(Long id, Client client) {
        Conversation conversation = new Conversation();
        conversation.setId(id);
        conversation.setClient(client);
        conversation.setRecipientId(55);
        conversation.setRecipientName("Maria Souza");
        conversation.setLastMessageContent("Oi");
        conversation.setLastMessageTime(Instant.parse("2026-01-01T10:00:00Z"));
        conversation.setUnreadCount(0);
        return conversation;
    }

    public static Message message(Long id, Conversation conversation, MessagePriority priority, MessageStatus status) {
        Message message = new Message();
        message.setId(id);
        message.setConversation(conversation);
        message.setSender(conversation.getClient());
        message.setRecipientId(conversation.getRecipientId());
        message.setContent("Olá");
        message.setPriority(priority);
        message.setStatus(status);
        message.setCost(priority == MessagePriority.URGENT ? new BigDecimal("0.50") : new BigDecimal("0.25"));
        message.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        return message;
    }
}
