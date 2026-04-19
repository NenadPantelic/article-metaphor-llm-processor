package com.article.metaphor_llm_processor.api.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record Document(String id,
                       String name,
                       String text,
                       String origin,
                       String path,
                       String type,
                       DocumentStatus status,
                       List<Metaphor> metaphors,
                       Instant createdAt,
                       Instant updatedAt) {
}

