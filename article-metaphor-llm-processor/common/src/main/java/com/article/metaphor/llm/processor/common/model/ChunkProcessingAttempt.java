package com.article.metaphor.llm.processor.common.model;

import java.time.Instant;

public record ChunkProcessingAttempt(Instant time,
                                     String error) {
}
