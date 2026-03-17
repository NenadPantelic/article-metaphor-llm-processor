package com.article.metaphor_llm_processor.chunk_processing.state_manager.service;

import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.inout.ChunkProcessingData;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.inout.ChunkProcessingStateData;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.model.ChunkProcessingState;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.repository.ChunkProcessingStateRepository;
import com.article.metaphor_llm_processor.common.exception.ApiException;
import com.article.metaphor_llm_processor.common.exception.ErrorReport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ChunkProcessingDataService {

    private final ChunkProcessingStateRepository chunkProcessingStateRepository;

    protected ChunkProcessingDataService(ChunkProcessingStateRepository chunkProcessingStateRepository) {
        this.chunkProcessingStateRepository = chunkProcessingStateRepository;
    }

    public ChunkProcessingStateData getProcessingInfo(String chunkId) {
        log.info("Fetching the processing info for chunkId: {}", chunkId);
        ChunkProcessingState chunkProcessingState = chunkProcessingStateRepository.findByChunkId(chunkId).orElseThrow(
                () -> new ApiException(ErrorReport.NOT_FOUND)
        );

        return new ChunkProcessingStateData(
                chunkProcessingState.getChunkId(),
                chunkProcessingState.getDocumentId(),
                mapToProcessingState(chunkProcessingState)
        );
    }

    public abstract void updateChunkProcessingStateData(ChunkProcessingState chunkProcessingState,
                                                        ChunkProcessingData chunkProcessingData);

    protected abstract ChunkProcessingData mapToProcessingState(ChunkProcessingState chunkProcessingState);

}
