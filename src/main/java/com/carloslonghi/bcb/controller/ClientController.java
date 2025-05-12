package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.config.docs.ClientControllerDocs;
import com.carloslonghi.bcb.dto.ClientBalanceDTO;
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
public class ClientController implements ClientControllerDocs {
    private final ClientService clientService;

    @PostMapping("/clients")
    public ResponseEntity<ClientDTO> create(@RequestBody ClientDTO dto) {
        ClientDTO clientCreated = clientService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(clientCreated);
    }

    @GetMapping("/clients")
    public ResponseEntity<List<ClientDTO>> findAll() {
        return ResponseEntity.ok(clientService.findAll());
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<ClientDTO> findById(@PathVariable Long id) {
        ClientDTO clientFound = clientService.findById(id);
        return ResponseEntity.ok(clientFound);
    }

    @PutMapping("/clients/{id}")
    public ResponseEntity<ClientDTO> updateById(@PathVariable Long id, @RequestBody ClientDTO dto) {
        ClientDTO clientUpdated = clientService.updateById(id, dto);
        return ResponseEntity.ok(clientUpdated);
    }

    @GetMapping("/clients/{id}/balance")
    public ResponseEntity<ClientBalanceDTO> getClientBalance(@PathVariable Long id) {
        ClientBalanceDTO clientBalance = clientService.getClientBalance(id);
        return ResponseEntity.ok(clientBalance);
    }
}
