package com.article.metaphor_llm_processor.api.dto.internal.identity;

import lombok.Builder;

@Builder
public record User(String id,
                   String displayName,
                   String username,
                   String password,
                   Role role) {
}
