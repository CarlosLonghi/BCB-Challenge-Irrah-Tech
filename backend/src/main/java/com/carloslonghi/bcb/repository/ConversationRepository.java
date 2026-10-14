package com.carloslonghi.bcb.repository;

import com.carloslonghi.bcb.entity.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    // Ordem vem do Pageable (ultima mensagem mais recente primeiro, id desempata)
    List<Conversation> findByClientId(Long clientId, Pageable pageable);
}
