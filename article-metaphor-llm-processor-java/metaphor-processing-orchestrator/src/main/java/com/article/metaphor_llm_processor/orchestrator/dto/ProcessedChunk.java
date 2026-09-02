package com.article.metaphor_llm_processor.orchestrator.dto;

import com.article.metaphor_llm_processor.orchestrator.model.ChunkProcessingError;

public record ProcessedChunk(String chunkId,
                             ChunkProcessingError error) {
}
