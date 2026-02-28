package com.article.metaphor_llm_processor.chunk_processing.state_manager.service;

import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.response.ChunkProcessingStateInfo;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.model.ChunkProcessingState;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.repository.ChunkProcessingStateRepository;
import com.article.metaphor_llm_processor.common.exception.ApiException;
import com.article.metaphor_llm_processor.common.exception.ErrorReport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ChunkProcessingInfoService {

    private final ChunkProcessingStateRepository chunkProcessingStateRepository;

    protected ChunkProcessingInfoService(ChunkProcessingStateRepository chunkProcessingStateRepository) {
        this.chunkProcessingStateRepository = chunkProcessingStateRepository;
    }

    public ChunkProcessingStateInfo getProcessingInfo(String chunkId) {
        log.info("Fetching the processing info for chunkId: {}", chunkId);
        ChunkProcessingState chunkProcessingState = chunkProcessingStateRepository.findByChunkId(chunkId).orElseThrow(
                () -> new ApiException(ErrorReport.NOT_FOUND)
        );
        return mapToProcessingInfo(chunkProcessingState);
    }

    protected abstract ChunkProcessingStateInfo mapToProcessingInfo(ChunkProcessingState chunkProcessingState);
}
