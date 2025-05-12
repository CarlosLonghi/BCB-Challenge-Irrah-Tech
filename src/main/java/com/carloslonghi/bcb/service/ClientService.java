package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.dto.ClientBalanceDTO;
import com.carloslonghi.bcb.dto.ClientDTO;
import com.carloslonghi.bcb.mapper.ClientMapper;
import com.carloslonghi.bcb.model.Client;
import com.carloslonghi.bcb.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    public ClientDTO create(ClientDTO dto) {
        Client client = clientMapper.toModel(dto);

        LocalDateTime now = LocalDateTime.now();
        client.setCreatedAt(now);
        client.setUpdatedAt(now);

        Client savedClient = clientRepository.save(client);
        return clientMapper.toDTO(savedClient);
    }

    public List<ClientDTO> findAll() {
        return clientRepository.findAll()
                .stream()
                .map(clientMapper::toDTO)
                .collect(Collectors.toList());
    }

    public ClientDTO findById(Long id) {
        Optional<Client> client = clientRepository.findById(id);
        return client.map(clientMapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cliente com id " + id + " não encontrado"
                ));
    }

    public ClientDTO updateById(Long id, ClientDTO dto) {
        clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cliente com id " + id + " não encontrado"
                ));

        Client clientUpdated = clientMapper.toModel(dto);
        clientUpdated.setId(id);

        Client saved = clientRepository.save(clientUpdated);
        return clientMapper.toDTO(saved);
    }

    public ClientBalanceDTO getClientBalance(Long id) {
        return clientRepository.findById(id)
                .map(client ->
                        new ClientBalanceDTO(client.getBalance(), client.getLimit())
                ).orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cliente com id " + id + " não encontrado"
                ));
    }
}
