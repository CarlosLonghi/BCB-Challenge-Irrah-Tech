package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.controller.api.spec.MessageApi;
import com.carloslonghi.bcb.controller.request.MessageRequest;
import com.carloslonghi.bcb.controller.response.MessageResponse;
import com.carloslonghi.bcb.controller.response.SendMessageResponse;
import com.carloslonghi.bcb.mapper.MessageMapper;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController implements MessageApi {

    private final MessageService messageService;
    private final MessageMapper messageMapper;

    @PostMapping
    public ResponseEntity<SendMessageResponse> send(@Valid @RequestBody MessageRequest request) {
        Message message = messageService.sendMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(messageMapper.toSendResponse(message));
    }

    @GetMapping
    public ResponseEntity<List<MessageResponse>> findAll() {
        List<MessageResponse> messages = messageService.findAll().stream()
                .map(messageMapper::toResponse)
                .toList();
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(messageMapper.toResponse(messageService.findById(id)));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<MessageStatus> getStatus(@PathVariable Long id) {
        return ResponseEntity.ok(messageService.getStatus(id));
    }
}
