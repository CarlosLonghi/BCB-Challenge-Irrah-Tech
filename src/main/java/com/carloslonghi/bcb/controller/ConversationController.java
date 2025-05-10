package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.dto.ConversationDTO;
import com.carloslonghi.bcb.dto.MessageDTO;
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
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    public ResponseEntity<List<ConversationDTO>> findAll() {
        return ResponseEntity.ok(conversationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(conversationService.findById(id));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageDTO>> findMessagesByConversationId(@PathVariable Long id) {
        return ResponseEntity.ok(conversationService.findMessagesByConversationId(id));
    }
}
