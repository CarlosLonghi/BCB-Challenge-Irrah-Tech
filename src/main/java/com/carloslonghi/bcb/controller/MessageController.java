package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.dto.MessageDTO;
import com.carloslonghi.bcb.dto.MessageRequestDTO;
import com.carloslonghi.bcb.dto.MessageResponseDTO;
import com.carloslonghi.bcb.model.enums.MessageStatus;
import com.carloslonghi.bcb.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageResponseDTO> send(
            @RequestBody MessageRequestDTO request) {
        MessageResponseDTO response = messageService.sendMessage(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<MessageDTO>> findAll() {
        return ResponseEntity.ok(messageService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(messageService.findById(id));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<MessageStatus> getStatus(@PathVariable Long id) {
        return ResponseEntity.ok(messageService.getStatus(id));
    }
}
