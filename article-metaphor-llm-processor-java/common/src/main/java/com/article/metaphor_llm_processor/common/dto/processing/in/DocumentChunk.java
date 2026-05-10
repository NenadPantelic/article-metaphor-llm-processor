package com.article.metaphor_llm_processor.common.dto.processing.in;

import com.article.metaphor_llm_processor.common.dto.processing.ChunkProcessingData;

public record DocumentChunk(String documentId,
                            String chunkId,
                            String text) implements ChunkProcessingData {

    @Override
    public String getState() {
        return null;
    }
}
