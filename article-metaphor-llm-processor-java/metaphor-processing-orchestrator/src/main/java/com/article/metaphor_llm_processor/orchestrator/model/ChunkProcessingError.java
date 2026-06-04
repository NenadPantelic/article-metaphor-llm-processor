package com.article.metaphor_llm_processor.orchestrator.model;

import java.time.Instant;

public record ChunkProcessingError(String error,
                                   Instant executedAt,
                                   ProcessingMilestone failedAtMilestone) {
}
