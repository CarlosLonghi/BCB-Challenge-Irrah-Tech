package com.carloslonghi.bcb.repository;

import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Acesso a tb_messages.
 * <p>
 * Status muda so pelos updates de coluna daqui ({@code @Modifying}), nunca por save() da entidade inteira, que
 * sobrescreveria a linha com uma copia antiga. Os updates tem {@code @Transactional} proprio porque o worker e as
 * rotinas agendadas os chamam fora de transacao; dentro de uma (ex.: estorno), entram nela.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    // Paginas por cursor (id), mais recentes primeiro; o cursor "before" e exclusivo
    List<Message> findBySenderIdOrderByIdDesc(Long senderId, Limit limit);
    List<Message> findBySenderIdAndIdLessThanOrderByIdDesc(Long senderId, Long before, Limit limit);
    List<Message> findByConversationIdOrderByIdDesc(Long conversationId, Limit limit);
    List<Message> findByConversationIdAndIdLessThanOrderByIdDesc(Long conversationId, Long before, Limit limit);

    // Recuperacao na inicializacao: pendentes, por ordem de criacao
    List<Message> findByStatusInOrderByCreatedAtAsc(List<MessageStatus> statuses);

    // GET /queue/status: todos os clientes, numa consulta so
    @Query("select new com.carloslonghi.bcb.repository.StatusCount(m.status, m.priority, count(m)) "
            + "from Message m group by m.status, m.priority")
    List<StatusCount> countByStatusAndPriority();

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

    // DELIVERED e a hora da entrega no mesmo update; o ReadSimulator usa deliveredAt para marcar READ
    @Transactional
    @Modifying
    @Query("update Message m set m.status = com.carloslonghi.bcb.entity.enums.MessageStatus.DELIVERED, m.deliveredAt = :at where m.id = :id")
    void markDelivered(Long id, Instant at);

    // Leitura simulada: as DELIVERED entregues ate "cutoff", com o dono de cada uma (para o evento SSE)
    @Query("select new com.carloslonghi.bcb.repository.MessageRef(m.id, m.sender.id, m.conversation.id) from Message m "
            + "where m.status = com.carloslonghi.bcb.entity.enums.MessageStatus.DELIVERED and m.deliveredAt <= :cutoff "
            + "order by m.id")
    List<MessageRef> findDeliveredBefore(Instant cutoff, Limit limit);

    // So troca as que ainda estao DELIVERED; devolve quantas mudaram
    @Transactional
    @Modifying
    @Query("update Message m set m.status = com.carloslonghi.bcb.entity.enums.MessageStatus.READ "
            + "where m.id in :ids and m.status = com.carloslonghi.bcb.entity.enums.MessageStatus.DELIVERED")
    int markRead(Collection<Long> ids);

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

    // Rotina periodica da recuperacao: commit feito, mas a publicacao no RabbitMQ falhou
    List<Message> findByStatusAndEnqueuedAtIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(MessageStatus status, Instant createdBefore);
}
