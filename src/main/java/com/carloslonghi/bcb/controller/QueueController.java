package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.dto.QueueStatusDTO;
import com.carloslonghi.bcb.service.QueueStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueStatusService queueStatusService;

    @GetMapping("/status")
    public ResponseEntity<QueueStatusDTO> getStatus() {
        return ResponseEntity.ok(queueStatusService.getStatus());
    }
}
