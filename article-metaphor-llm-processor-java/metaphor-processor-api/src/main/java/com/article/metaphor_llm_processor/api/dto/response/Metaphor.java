package com.article.metaphor_llm_processor.api.dto.response;

import lombok.Builder;

@Builder
public record Metaphor(int offset,
                       int length,
                       String phrase,
                       MetaphorType type,
                       String explanation) {
}
