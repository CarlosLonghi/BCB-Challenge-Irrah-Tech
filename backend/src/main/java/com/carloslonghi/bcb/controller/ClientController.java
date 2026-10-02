package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.controller.api.spec.ClientApi;
import com.carloslonghi.bcb.controller.request.ClientRequest;
import com.carloslonghi.bcb.controller.request.ClientUpdateRequest;
import com.carloslonghi.bcb.controller.response.ClientBalanceResponse;
import com.carloslonghi.bcb.controller.response.ClientResponse;
import com.carloslonghi.bcb.mapper.ClientMapper;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ClientController implements ClientApi {

    private final ClientService clientService;
    private final ClientMapper clientMapper;

    @PostMapping("/clients")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        Client clientCreated = clientService.create(clientMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(clientMapper.toResponse(clientCreated));
    }

    @GetMapping("/clients")
    public ResponseEntity<List<ClientResponse>> findAll() {
        List<ClientResponse> clients = clientService.findAuthenticated().stream()
                .map(clientMapper::toResponse)
                .toList();
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<ClientResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(clientMapper.toResponse(clientService.findById(id)));
    }

    @PutMapping("/clients/{id}")
    public ResponseEntity<ClientResponse> updateById(@PathVariable Long id, @Valid @RequestBody ClientUpdateRequest request) {
        Client clientUpdated = clientService.updateName(id, request.name());
        return ResponseEntity.ok(clientMapper.toResponse(clientUpdated));
    }

    @GetMapping("/clients/{id}/balance")
    public ResponseEntity<ClientBalanceResponse> getClientBalance(@PathVariable Long id) {
        return ResponseEntity.ok(clientMapper.toBalanceResponse(clientService.findById(id)));
    }
}
