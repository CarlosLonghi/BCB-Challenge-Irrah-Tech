package com.carloslonghi.bcb.repository;

import com.carloslonghi.bcb.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByClientId(Long senderId);
}
