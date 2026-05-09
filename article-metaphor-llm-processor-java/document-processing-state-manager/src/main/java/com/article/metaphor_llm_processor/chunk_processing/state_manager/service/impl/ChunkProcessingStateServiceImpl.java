package com.article.metaphor_llm_processor.chunk_processing.state_manager.service.impl;

import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.in.ChunkProcessingError;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.in.ChunkProcessingStateUpdate;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.out.ChunkProcessingStateData;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.out.ChunkStateUpdateResult;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.model.ChunkProcessingState;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.repository.ChunkProcessingStateRepository;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.service.ChunkProcessingDataService;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.service.ChunkProcessingStateService;
import com.article.metaphor_llm_processor.common.dto.processing.ChunkProcessingData;
import com.article.metaphor_llm_processor.common.exception.ApiException;
import com.article.metaphor_llm_processor.common.exception.DocumentProcessingStateManagerApiException;
import com.article.metaphor_llm_processor.common.exception.ErrorReport;
import com.article.metaphor_llm_processor.common.model.ChunkProcessingAttempt;
import com.article.metaphor_llm_processor.common.model.DocumentChunkState;
import com.article.metaphor_llm_processor.common.model.IndexedDocumentChunk;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ChunkProcessingStateServiceImpl implements ChunkProcessingStateService {

    private final Map<String, ChunkProcessingDataService> stateServiceMap;

    private final ChunkProcessingStateRepository chunkProcessingStateRepository;
    private final IndexedDocumentChunkRepository chunkRepository;

    public ChunkProcessingStateServiceImpl(LexicalUnitProcessingDataService lexicalUnitProcessingInfoService,
                                           LemmaMeaningProcessorDataService lemmaMeaningProcessorDataService,
                                           ChunkProcessingStateRepository chunkProcessingStateRepository,
                                           IndexedDocumentChunkRepository chunkRepository) {
        this.chunkProcessingStateRepository = chunkProcessingStateRepository;
        this.chunkRepository = chunkRepository;
        this.stateServiceMap = new HashMap<>();

        stateServiceMap.put(DocumentChunkState.LEXICAL_UNIT_PROCESSING__IN_PROGRESS.name(), lexicalUnitProcessingInfoService);
        stateServiceMap.put(DocumentChunkState.LEXICAL_UNIT_PROCESSING__COMPLETE.name(), lexicalUnitProcessingInfoService);
        stateServiceMap.put(DocumentChunkState.LEXICAL_UNIT_PROCESSING__FAILED.name(), lexicalUnitProcessingInfoService);

        stateServiceMap.put(DocumentChunkState.DICTIONARY_ACCESS__IN_PROGRESS.name(), lemmaMeaningProcessorDataService);
        stateServiceMap.put(DocumentChunkState.DICTIONARY_ACCESS__COMPLETE.name(), lemmaMeaningProcessorDataService);
        stateServiceMap.put(DocumentChunkState.DICTIONARY_ACCESS__FAILED.name(), lemmaMeaningProcessorDataService);
    }

    @Transactional
    @Override
    public ChunkStateUpdateResult updateChunkProcessingState(String chunkId, ChunkProcessingStateUpdate newState) {
        log.info("Updating the chunk processing state: chunkId = {}, data = {}", chunkId, newState);

        ChunkProcessingState chunkProcessingState = chunkProcessingStateRepository.findByChunkId(chunkId)
                .orElseThrow(
                        () -> new ApiException(ErrorReport.NOT_FOUND)
                );
        IndexedDocumentChunk chunk = chunkRepository.findById(chunkId)
                .orElseThrow(
                        () -> new ApiException(ErrorReport.NOT_FOUND)
                );
        DocumentChunkState chunkState = parseChunkState(newState.state());
        ChunkProcessingError error = newState.error();

        if (error != null) {
            ChunkProcessingAttempt attempt = new ChunkProcessingAttempt(
                    newState.processingTime(),
                    newState.error().error(),
                    chunkState.getMilestone()
            );

            chunk.addFailedAttempt(attempt);
        } else {
            ChunkProcessingDataService chunkProcessingDataService = getDataServiceOrThrowException(newState.state());
            ChunkProcessingData data = newState.data();
            if (data != null) {
                chunkProcessingDataService.updateChunkProcessingStateData(chunkProcessingState, newState.data());
                chunkProcessingState = chunkProcessingStateRepository.save(chunkProcessingState);
            }
        }

        long numOfUpdated = chunkRepository.updateChunkState(chunkId, chunkState.name());
        if (numOfUpdated == 0) {
            throw new ApiException(ErrorReport.INTERNAL_SERVER_ERROR);
        }

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

    private DocumentChunkState parseChunkState(String state) {
        try {
            return DocumentChunkState.valueOf(state);
        } catch (IllegalArgumentException e) {
            throw new DocumentProcessingStateManagerApiException(
                    String.format("Unable to parse an invalid string to a chunk state: %s", state)
            );
        }
    }
}
