package com.carloslonghi.bcb.mapper;

import com.carloslonghi.bcb.dto.ClientDTO;
import com.carloslonghi.bcb.model.Client;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ClientMapper {
    public Client toModel(ClientDTO dto) {
        Client client = new Client();
        client.setId(dto.getId());
        client.setName(dto.getName());
        client.setDocument(dto.getDocument());
        client.setDocumentType(dto.getDocumentType());
        client.setPlanType(dto.getPlanType());
        client.setBalance(dto.getBalance());
        client.setLimit(dto.getLimit());
        client.setActive(dto.isActive());

        return client;
    }

    public ClientDTO toDTO(Client client) {
        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setName(client.getName());
        dto.setDocument(client.getDocument());
        dto.setDocumentType(client.getDocumentType());
        dto.setPlanType(client.getPlanType());
        dto.setBalance(client.getBalance());
        dto.setLimit(client.getLimit());
        dto.setActive(client.isActive());

        return dto;
    }
}
