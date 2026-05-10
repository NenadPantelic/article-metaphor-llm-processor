package com.article.metaphor_llm_processor.chunk_processing.state_manager.service;

import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.in.ChunkProcessingStateUpdate;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.out.ChunkStateUpdateResult;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.out.ChunkProcessingStateData;

public interface ChunkProcessingStateService {

    ChunkStateUpdateResult updateChunkProcessingState(String chunkId, ChunkProcessingStateUpdate newState);

    ChunkProcessingStateData getChunkProcessingInfo(String chunkId, String stage);
}
