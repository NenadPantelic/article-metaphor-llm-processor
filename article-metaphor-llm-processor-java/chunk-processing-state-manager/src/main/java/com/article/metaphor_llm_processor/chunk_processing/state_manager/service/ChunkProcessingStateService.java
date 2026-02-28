package com.article.metaphor_llm_processor.chunk_processing.state_manager.service;

import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.response.ChunkProcessingStateInfo;

public interface ChunkProcessingStateService {

    void updateChunkProcessingState(String chunkId, Object statePayload);

    ChunkProcessingStateInfo getChunkProcessingInfo(String chunkId, String stage);
}
