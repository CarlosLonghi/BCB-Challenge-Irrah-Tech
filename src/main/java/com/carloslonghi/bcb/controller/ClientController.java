package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.dto.ClientDTO;
import com.carloslonghi.bcb.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ClientController {
    private final ClientService clientService;

    @PostMapping("/clients")
    public ResponseEntity<ClientDTO> create(@RequestBody ClientDTO dto) {
        ClientDTO clientCreated = clientService.create(dto);

        return ResponseEntity.ok(clientCreated);
    }

    @GetMapping("/clients")
    public ResponseEntity<List<ClientDTO>> findAll() {
        return ResponseEntity.ok(clientService.findAll());
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        ClientDTO clientFound = clientService.findById(id);
        if (clientFound != null) {
            return ResponseEntity.ok(clientFound);
        }

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("Cliente com id " + id +  " não encontrado");
    }

    @PutMapping("/clients/{id}")
    public ResponseEntity<?> updateById(@PathVariable Long id, @RequestBody ClientDTO dto) {
        ClientDTO clientFound = clientService.findById(id);
        if (clientFound != null) {
            ClientDTO clientUpdated = clientService.updateById(id, dto);
            return ResponseEntity
                    .ok(clientUpdated);
        }
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("Cliente com id " + id + " não encontrado");
    }
}
