package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.dto.ConversationDTO;
import com.carloslonghi.bcb.dto.MessageDTO;
import com.carloslonghi.bcb.mapper.ConversationMapper;
import com.carloslonghi.bcb.mapper.MessageMapper;
import com.carloslonghi.bcb.model.Conversation;
import com.carloslonghi.bcb.model.Message;
import com.carloslonghi.bcb.model.enums.MessageStatus;
import com.carloslonghi.bcb.repository.ClientRepository;
import com.carloslonghi.bcb.repository.ConversationRepository;
import com.carloslonghi.bcb.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    public List<ConversationDTO> findAll() {
        Long authId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        List<Conversation> conversations = conversationRepository.findByClientId(authId);

        return conversations.stream()
                .map(conversationMapper::toDTO)
                .collect(Collectors.toList());
    }

    public ConversationDTO findById(Long id) {
        Long authId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Conversation conversation = conversationRepository.findById(id)
                .filter(msg -> msg.getClient().getId().equals(authId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensagem não encontrada"));

        return conversationMapper.toDTO(conversation);
    }

    public List<MessageDTO> findMessagesByConversationId(Long conversationId) {
        Long authId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Conversation conversation = conversationRepository.findById(conversationId)
                .filter(conv -> conv.getClient().getId().equals(authId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversa não encontrada"));

        List<Message> messages = messageRepository.findByConversationId(conversation.getId());

        return messages.stream()
                .map(messageMapper::toDTO)
                .collect(Collectors.toList());
    }
}
