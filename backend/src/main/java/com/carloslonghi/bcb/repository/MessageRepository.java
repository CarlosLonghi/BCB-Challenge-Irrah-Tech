package com.carloslonghi.bcb.repository;

import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
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

    // Troca o status so se o atual estiver em "from"; devolve 1 se trocou e 0 se outra chamada chegou antes
    @Transactional
    @Modifying
    @Query("update Message m set m.status = :to where m.id = :id and m.status in :from")
    int updateStatusIfIn(Long id, Collection<MessageStatus> from, MessageStatus to);

    // Volta para QUEUED como "nao publicada": se a publicacao seguinte falhar, a rotina periodica a encontra
    @Transactional
    @Modifying
    @Query("update Message m set m.status = com.carloslonghi.bcb.entity.enums.MessageStatus.QUEUED, m.enqueuedAt = null where m.id = :id")
    void resetToUnpublished(Long id);

    // REQUIRES_NEW: e chamado no afterCommit do envio, quando a transacao original ja terminou mas ainda
    // esta vinculada a thread; com REQUIRED o update entraria nela e falharia ("No active transaction")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying
    @Query("update Message m set m.enqueuedAt = :enqueuedAt where m.id = :id")
    void markEnqueued(Long id, Instant enqueuedAt);

    // Commit feito, mas a publicacao no RabbitMQ falhou
    List<Message> findByStatusAndEnqueuedAtIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(MessageStatus status, Instant createdBefore);
}
