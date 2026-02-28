package com.article.metaphor_llm_processor.chunk_processing.state_manager.service.impl;

import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.response.ChunkProcessingStateInfo;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.model.ChunkProcessingState;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.service.ChunkProcessingInfoService;
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

    private Map<String, ChunkProcessingInfoService> stateServiceMap;

    private final LexicalUnitProcessingInfoService lexicalUnitProcessingInfoService;

    public ChunkProcessingStateServiceImpl(LexicalUnitProcessingInfoService lexicalUnitProcessingInfoService) {
        this.lexicalUnitProcessingInfoService = lexicalUnitProcessingInfoService;
        this.stateServiceMap = new HashMap<>();

        stateServiceMap.put(DocumentChunkStatus.STARTED_PROCESSING.name(), lexicalUnitProcessingInfoService);
        stateServiceMap.put(DocumentChunkStatus.LEXICAL_UNIT_PROCESSING__FAILED.name(), lexicalUnitProcessingInfoService);
    }

    @Override
    public void updateChunkProcessingState(String chunkId, Object statePayload) {

    }

    @Override
    public ChunkProcessingStateInfo getChunkProcessingInfo(String chunkId, String stage) {
        log.info("Fetching the chunk processing info: chunkId = {}, stage = {}", chunkId, stage);

        ChunkProcessingInfoService chunkProcessingInfoService = stateServiceMap.get(stage);
        if (chunkProcessingInfoService == null) {
            throw new ApiException(ErrorReport.BAD_REQUEST.withMessage("Invalid stage"));
        }

        return chunkProcessingInfoService.getProcessingInfo(chunkId);
    }
}
