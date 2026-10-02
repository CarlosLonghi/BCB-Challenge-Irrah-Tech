package com.carloslonghi.bcb.repository;

import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySenderId(Long senderId);
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    List<Message> findByStatusInOrderByCreatedAtAsc(List<MessageStatus> statuses);
    int countByStatus(MessageStatus status);
    int countByStatusAndPriority(MessageStatus status, MessagePriority priority);

    // Atualiza so a coluna status, sem sobrescrever o resto da linha com uma copia antiga da entidade
    @Transactional
    @Modifying
    @Query("update Message m set m.status = :status where m.id = :id")
    void updateStatus(Long id, MessageStatus status);
}
