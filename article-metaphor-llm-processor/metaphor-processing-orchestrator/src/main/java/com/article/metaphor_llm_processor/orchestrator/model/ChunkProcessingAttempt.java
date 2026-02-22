package com.article.metaphor.llm.processor.orchestrator.model;

import com.article.metaphor.llm.processor.common.model.DocumentChunkStatus;

import java.time.Instant;

public record ChunkProcessingAttempt(Instant time,
                                     String error,
                                     // what's the milestone at which it failed
                                     DocumentChunkStatus milestoneWhereFailed) {
}
