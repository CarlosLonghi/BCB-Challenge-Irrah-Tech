package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.config.JwtProperties;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.exception.ClientNotActiveException;
import com.carloslonghi.bcb.exception.InvalidCredentialsException;
import com.carloslonghi.bcb.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Login por documento e senha e emissao do JWT de sessao (POST /auth).
 * A validacao do token nas demais rotas nao passa por aqui: e feita pelo oauth2ResourceServer do SecurityConfig.
 */
@Service
@RequiredArgsConstructor
public class AuthService {
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    // Documento inexistente, cliente sem senha e senha errada caem no mesmo 401, sem revelar qual foi.
    // Inativo (403) so e informado depois da senha certa
    public Client authenticate(String document, String password) {
        Client client = clientRepository.findByDocument(document)
                .filter(found -> found.getPasswordHash() != null)
                .filter(found -> passwordEncoder.matches(password, found.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        if (!client.isActive()) {
            throw new ClientNotActiveException();
        }

        return client;
    }

    // JWT assinado com o id do cliente no "sub"; vale ate "exp" e sobrevive a um restart do backend
    public String createToken(Long clientId) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(clientId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.ttl()))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
