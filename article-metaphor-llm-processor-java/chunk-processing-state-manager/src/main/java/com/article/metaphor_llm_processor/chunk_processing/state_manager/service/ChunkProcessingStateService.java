package com.article.metaphor_llm_processor.chunk_processing.state_manager.service;

import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.inout.ChunkProcessingStateData;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.out.ChunkStateUpdateResult;

public interface ChunkProcessingStateService {

    ChunkStateUpdateResult updateChunkProcessingState(String chunkId, ChunkProcessingStateData newState);

    ChunkProcessingStateData getChunkProcessingInfo(String chunkId, String stage);
}
