package com.article.metaphor_llm_processor.api.auth.identity;

import com.article.metaphor_llm_processor.api.dto.internal.identity.Role;

public record Identity(String userId,
                       String username,
                       Role role) {
}
