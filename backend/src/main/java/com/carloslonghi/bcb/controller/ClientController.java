package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.controller.api.spec.ClientApi;
import com.carloslonghi.bcb.controller.request.ClientRequest;
import com.carloslonghi.bcb.controller.request.ClientUpdateRequest;
import com.carloslonghi.bcb.controller.response.ClientBalanceResponse;
import com.carloslonghi.bcb.controller.response.ClientResponse;
import com.carloslonghi.bcb.controller.response.LedgerEntryResponse;
import com.carloslonghi.bcb.mapper.ClientMapper;
import com.carloslonghi.bcb.mapper.LedgerEntryMapper;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.service.ClientService;
import com.carloslonghi.bcb.service.LedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.carloslonghi.bcb.utils.PageLimit;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Rotas /clients. POST /clients (cadastro) e publico; as demais so acessam o proprio cliente (403 para outro id).
 * Regras no ClientService e no LedgerService; documentacao em ClientApi.
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
public class ClientController implements ClientApi {

    private final ClientService clientService;
    private final ClientMapper clientMapper;
    private final LedgerService ledgerService;
    private final LedgerEntryMapper ledgerEntryMapper;

    @PostMapping("/clients")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        Client clientCreated = clientService.create(clientMapper.toEntity(request), request.password());
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

    @GetMapping("/clients/{id}/ledger")
    public ResponseEntity<List<LedgerEntryResponse>> getLedger(@PathVariable Long id,
                                                               @RequestParam(required = false) Long before,
                                                               @RequestParam(defaultValue = "" + PageLimit.DEFAULT) int limit) {
        List<LedgerEntryResponse> entries = ledgerService.findByClient(id, before, limit).stream()
                .map(ledgerEntryMapper::toResponse)
                .toList();
        return ResponseEntity.ok(entries);
    }
}
