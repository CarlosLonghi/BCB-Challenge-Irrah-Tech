package com.carloslonghi.bcb.controller;

import com.carloslonghi.bcb.config.docs.AuthControllerDocs;
import com.carloslonghi.bcb.dto.AuthRequest;
import com.carloslonghi.bcb.dto.AuthResponse;
import com.carloslonghi.bcb.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;

    @PostMapping
    public ResponseEntity<AuthResponse> authenticate(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }
}
