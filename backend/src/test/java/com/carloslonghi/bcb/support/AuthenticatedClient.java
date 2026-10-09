package com.carloslonghi.bcb.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

// Simula o que o TokenAuthenticationFilter faz: o id do cliente vira o principal do contexto
public final class AuthenticatedClient {

    private AuthenticatedClient() {
    }

    public static void login(Long clientId) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(clientId, null, List.of()));
    }

    public static void logout() {
        SecurityContextHolder.clearContext();
    }
}
