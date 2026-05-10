package com.article.metaphor_llm_processor.api.service.impl;

import com.article.metaphor_llm_processor.api.auth.AuthHandler;
import com.article.metaphor_llm_processor.api.auth.identity.IdentitySession;
import com.article.metaphor_llm_processor.api.auth.identity.IdentitySessionContextHolder;
import com.article.metaphor_llm_processor.api.dto.request.AuthRequest;
import com.article.metaphor_llm_processor.api.dto.response.AuthResponse;
import com.article.metaphor_llm_processor.api.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final AuthHandler authHandler;

    public AuthServiceImpl(AuthHandler authHandler) {
        this.authHandler = authHandler;
    }

    @Override
    public AuthResponse login(AuthRequest authRequest) {
        log.info("Authenticating user: user={}", authRequest.username());
        String credential = authHandler.authenticate(authRequest.username(), authRequest.password());
        return new AuthResponse(credential);
    }

    @Override
    public void logout() {
        IdentitySession identitySession = IdentitySessionContextHolder.get();
        log.info("Logging out identity = {}", identitySession.identity());
        authHandler.clearAuthentication(IdentitySessionContextHolder.get().credential());
    }
}
