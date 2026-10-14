package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.controller.request.MessageRequest;
import com.carloslonghi.bcb.exception.ReferencedEntityNotFoundException;
import com.carloslonghi.bcb.infra.events.MessageStatusChanged;
import com.carloslonghi.bcb.infra.queue.MessageQueue;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.entity.Conversation;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.LedgerEntryType;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.repository.ClientRepository;
import com.carloslonghi.bcb.repository.ConversationRepository;
import com.carloslonghi.bcb.repository.MessageRepository;
import com.carloslonghi.bcb.utils.PageLimit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ClientRepository clientRepository;

    private final ClientService clientService;
    private final ConversationService conversationService;
    private final LedgerService ledgerService;

    private final MessageQueue messageQueue;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    private static final Set<MessageStatus> PENDING_STATUSES =
            Set.of(MessageStatus.QUEUED, MessageStatus.PROCESSING, MessageStatus.SENT);

    private static final BigDecimal NORMAL_COST = new BigDecimal("0.25");
    private static final BigDecimal URGENT_COST = new BigDecimal("0.50");

    // Debito, mensagem e conversa sao gravados juntos: se algo falhar, nada e cobrado
    @Transactional
    public Message sendMessage(MessageRequest request) {
        Long senderId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Client sender = clientRepository.findWithLockById(senderId)
                .orElseThrow(() -> new ReferencedEntityNotFoundException("Cliente", senderId));

        Conversation conversation = conversationService.findOrCreate(sender, request);

        BigDecimal cost = request.priority() == MessagePriority.URGENT ? URGENT_COST : NORMAL_COST;
        clientService.debit(sender, cost);

        Message message = messageRepository.save(newMessage(sender, conversation, request, cost));
        ledgerService.record(sender, message, LedgerEntryType.DEBIT, cost);

        conversation.setLastMessageContent(message.getContent());
        conversation.setLastMessageTime(message.getCreatedAt());
        conversationRepository.save(conversation);

        enqueueAfterCommit(message);
        return message;
    }

    // Chamado quando as tentativas do worker acabam: marca FAILED e devolve o custo, tudo numa transacao.
    // So estorna quem tirou a mensagem de pendente; se ela ja estava finalizada (ex.: DLQ reprocessada), nada muda
    @Transactional
    public void failAndRefund(Long messageId) {
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message == null) {
            log.warn("Mensagem {} nao existe; nada a estornar", messageId);
            return;
        }

        // Mesmo lock do envio: estorno e envio simultaneos do mesmo cliente nao se atropelam no saldo
        Client sender = clientRepository.findWithLockById(message.getSender().getId())
                .orElseThrow(() -> new ReferencedEntityNotFoundException("Cliente", message.getSender().getId()));

        if (messageRepository.updateStatusIfIn(messageId, PENDING_STATUSES, MessageStatus.FAILED) == 0) {
            return;
        }

        clientService.refund(sender, message.getCost());
        ledgerService.record(sender, message, LedgerEntryType.REFUND, message.getCost());
        // Entregue ao SSE so depois do commit (o frontend recarrega o saldo ao receber o FAILED)
        events.publishEvent(new MessageStatusChanged(
                sender.getId(), message.getConversation().getId(), messageId, MessageStatus.FAILED));
    }

    // Mensagens do cliente, mais recente primeiro, paginadas por id
    public List<Message> findAll(Long before, int limit) {
        Long authId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return before == null
                ? messageRepository.findBySenderIdOrderByIdDesc(authId, PageLimit.of(limit))
                : messageRepository.findBySenderIdAndIdLessThanOrderByIdDesc(authId, before, PageLimit.of(limit));
    }

    public Message findById(Long id) {
        Long authId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return messageRepository.findById(id)
                .filter(msg -> msg.getSender().getId().equals(authId))
                .orElseThrow(() -> new ReferencedEntityNotFoundException("Mensagem", id));
    }

    public MessageStatus getStatus(Long id) {
        return findById(id).getStatus();
    }

    private Message newMessage(Client sender, Conversation conversation, MessageRequest request, BigDecimal cost) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setRecipientId(conversation.getRecipientId());
        message.setContent(request.content());
        message.setPriority(request.priority());
        message.setStatus(MessageStatus.QUEUED);
        message.setCost(cost);
        message.setCreatedAt(clock.instant());
        return message;
    }

    // So enfileira depois do commit, senao o worker poderia pegar uma mensagem que ainda nao existe no banco
    private void enqueueAfterCommit(Message message) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // O debito ja foi gravado; se a fila estiver fora do ar a mensagem fica QUEUED com enqueued_at
                // nulo, e a rotina periodica do PendingMessagesRecovery a publica de novo
                try {
                    messageQueue.enqueue(message);
                } catch (Exception e) {
                    log.error("Nao foi possivel enfileirar a mensagem {}", message.getId(), e);
                    return;
                }
                // Se so a marcacao falhar, a rotina periodica publica de novo; o worker ignora a duplicata
                try {
                    messageRepository.markEnqueued(message.getId(), clock.instant());
                } catch (Exception e) {
                    log.warn("Mensagem {} publicada, mas sem enqueued_at; pode ser publicada de novo", message.getId(), e);
                }
            }
        });
    }
}
