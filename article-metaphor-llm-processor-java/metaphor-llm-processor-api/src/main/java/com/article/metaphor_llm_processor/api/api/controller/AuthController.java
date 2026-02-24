package com.article.metaphor_llm_processor.api.api.controller;

import com.article.metaphor_llm_processor.api.dto.request.AuthRequest;
import com.article.metaphor_llm_processor.api.dto.response.AuthResponse;
import com.article.metaphor_llm_processor.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid AuthRequest authRequest) {
        log.info("Login request: user={}", authRequest.username());
        AuthResponse authResponse = authService.login(authRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/logout")
    public void login() {
        log.info("Logout request received...");
        authService.logout();
    }

}
