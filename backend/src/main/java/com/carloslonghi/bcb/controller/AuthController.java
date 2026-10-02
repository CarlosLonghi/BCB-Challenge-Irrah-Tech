package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.controller.api.spec.AuthApi;
import com.carloslonghi.bcb.controller.request.AuthRequest;
import com.carloslonghi.bcb.controller.response.AuthResponse;
import com.carloslonghi.bcb.mapper.ClientMapper;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;
    private final ClientMapper clientMapper;

    @PostMapping
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request) {
        Client client = authService.authenticate(request.document());
        String token = authService.createToken(client.getId());

        return ResponseEntity.ok(new AuthResponse(token, clientMapper.toResponse(client)));
    }
}
