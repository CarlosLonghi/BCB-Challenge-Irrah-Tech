package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.controller.api.spec.ConversationApi;
import com.carloslonghi.bcb.controller.response.ConversationResponse;
import com.carloslonghi.bcb.controller.response.MessageResponse;
import com.carloslonghi.bcb.mapper.ConversationMapper;
import com.carloslonghi.bcb.mapper.MessageMapper;
import com.carloslonghi.bcb.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController implements ConversationApi {

    private final ConversationService conversationService;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> findAll() {
        List<ConversationResponse> conversations = conversationService.findAll().stream()
                .map(conversationMapper::toResponse)
                .toList();
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(conversationMapper.toResponse(conversationService.findById(id)));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponse>> findMessagesByConversationId(@PathVariable Long id) {
        List<MessageResponse> messages = conversationService.findMessages(id).stream()
                .map(messageMapper::toResponse)
                .toList();
        return ResponseEntity.ok(messages);
    }
}
