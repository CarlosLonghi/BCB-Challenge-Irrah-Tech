package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.exception.ClientNotActiveException;
import com.carloslonghi.bcb.exception.DocumentNotRegisteredException;
import com.carloslonghi.bcb.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final ClientRepository clientRepository;
    private final Map<String, Long> tokenStore = new ConcurrentHashMap<>();

    public Client authenticate(String document) {
        Client client = clientRepository.findByDocument(document)
                .orElseThrow(DocumentNotRegisteredException::new);

        if (!client.isActive()) {
            throw new ClientNotActiveException();
        }

        return client;
    }

    public String createToken(Long clientId) {
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, clientId);
        return token;
    }

    public Long getClientIdFromToken(String token) {
        return tokenStore.get(token);
    }
}
