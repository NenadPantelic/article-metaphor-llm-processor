package com.article.metaphor_llm_processor.orchestrator.dto;

public record ReprocessingMessage(String chunkId,
                                  ReprocessingType typeOfReprocessing) implements PipelineMessage {
    @Override
    public String getChunkId() {
        return chunkId;
    }
}
