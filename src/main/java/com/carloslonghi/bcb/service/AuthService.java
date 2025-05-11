package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.dto.AuthRequest;
import com.carloslonghi.bcb.dto.AuthResponse;
import com.carloslonghi.bcb.model.Client;
import com.carloslonghi.bcb.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final ClientRepository clientRepository;
    private final Map<String, Long> tokenStore = new ConcurrentHashMap<>();

    public AuthResponse authenticate(AuthRequest request) {
        Client client = clientRepository.findByDocument(request.getDocument())
                .orElseThrow(() -> new UsernameNotFoundException("Documento inválido. Verifique o CPF/CNPJ"));

        String token = UUID.randomUUID().toString();
        tokenStore.put(token, client.getId());

        return new AuthResponse(token);
    }

    public Long getClientIdFromToken(String token) {
        return tokenStore.get(token);
    }
}
