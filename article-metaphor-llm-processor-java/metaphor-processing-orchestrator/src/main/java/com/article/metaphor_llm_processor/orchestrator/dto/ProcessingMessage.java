package com.article.metaphor_llm_processor.orchestrator.dto;

public record ProcessingMessage(String chunkId,
                                String text) implements PipelineMessage {
    @Override
    public String getChunkId() {
        return chunkId;
    }
}
