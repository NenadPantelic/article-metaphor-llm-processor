package com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.response;

public record ChunkProcessingStateInfo(String chunkId,
                                       String documentId,
                                       String status) {
}
