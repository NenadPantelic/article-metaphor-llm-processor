package com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.in;

public record ChunkProcessingError(String error,
                                   boolean reprocessable) {
}
