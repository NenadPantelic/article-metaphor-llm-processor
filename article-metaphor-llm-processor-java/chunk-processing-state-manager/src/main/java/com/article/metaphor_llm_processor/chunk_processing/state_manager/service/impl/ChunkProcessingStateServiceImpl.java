package com.article.metaphor_llm_processor.chunk_processing.state_manager.service.impl;

import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.inout.ChunkProcessingStateData;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.out.ChunkStateUpdateResult;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.model.ChunkProcessingState;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.repository.ChunkProcessingStateRepository;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.service.ChunkProcessingDataService;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.service.ChunkProcessingStateService;
import com.article.metaphor_llm_processor.common.exception.ApiException;
import com.article.metaphor_llm_processor.common.exception.ErrorReport;
import com.article.metaphor_llm_processor.common.model.DocumentChunkStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ChunkProcessingStateServiceImpl implements ChunkProcessingStateService {

    private final Map<String, ChunkProcessingDataService> stateServiceMap;

    private final ChunkProcessingStateRepository chunkProcessingStateRepository;

    public ChunkProcessingStateServiceImpl(LexicalUnitProcessingDataService lexicalUnitProcessingInfoService,
                                           ChunkProcessingStateRepository chunkProcessingStateRepository) {
        this.chunkProcessingStateRepository = chunkProcessingStateRepository;
        this.stateServiceMap = new HashMap<>();

        stateServiceMap.put(DocumentChunkStatus.STARTED_PROCESSING.name(), lexicalUnitProcessingInfoService);
        stateServiceMap.put(DocumentChunkStatus.LEXICAL_UNIT_PROCESSING__FAILED.name(), lexicalUnitProcessingInfoService);
    }

    @Override
    public ChunkStateUpdateResult updateChunkProcessingState(String chunkId, ChunkProcessingStateData newState) {
        log.info("Updating the chunk processing state: chunkId = {}, data = {}", chunkId, newState);

        ChunkProcessingState chunkProcessingState = chunkProcessingStateRepository.findByChunkId(chunkId)
                .orElseThrow(
                        () -> new ApiException(ErrorReport.NOT_FOUND)
                );

        ChunkProcessingDataService chunkProcessingDataService = getDataServiceOrThrowException(
                newState.data().getState()
        );
        chunkProcessingDataService.updateChunkProcessingStateData(chunkProcessingState, newState.data());
        chunkProcessingState = chunkProcessingStateRepository.save(chunkProcessingState);
        return new ChunkStateUpdateResult(
                chunkProcessingState.getChunkId(),
                chunkProcessingState.getState()
        );
    }

    @Override
    public ChunkProcessingStateData getChunkProcessingInfo(String chunkId, String state) {
        log.info("Fetching the chunk processing info: chunkId = {}, stage = {}", chunkId, state);
        ChunkProcessingDataService chunkProcessingDataService = getDataServiceOrThrowException(state);
        return chunkProcessingDataService.getProcessingInfo(chunkId);
    }

    private ChunkProcessingDataService getDataServiceOrThrowException(String state) {
        ChunkProcessingDataService chunkProcessingDataService = stateServiceMap.get(state);
        if (chunkProcessingDataService == null) {
            throw new ApiException(ErrorReport.BAD_REQUEST.withMessage(String.format("Invalid state: %s", state)));
        }

        return chunkProcessingDataService;
    }
}
