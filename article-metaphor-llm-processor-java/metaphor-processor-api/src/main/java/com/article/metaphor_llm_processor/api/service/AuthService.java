package com.article.metaphor_llm_processor.api.service;

import com.article.metaphor_llm_processor.api.dto.request.AuthRequest;
import com.article.metaphor_llm_processor.api.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(AuthRequest authRequest);

    void logout();
}
