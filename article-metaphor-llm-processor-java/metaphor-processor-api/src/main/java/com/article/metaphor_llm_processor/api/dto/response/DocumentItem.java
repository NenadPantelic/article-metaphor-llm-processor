package com.article.metaphor_llm_processor.api.dto.response;

import lombok.Builder;

@Builder
public record DocumentItem(String id,
                           String name,
                           DocumentStatus status,
                           String origin) {
}
