package com.carloslonghi.bcb.repository;

import com.carloslonghi.bcb.model.Message;
import com.carloslonghi.bcb.model.enums.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySenderId(Long senderId);
    List<Message> findByConversationId(Long conversationId);
    int countByStatus(MessageStatus status);
}
