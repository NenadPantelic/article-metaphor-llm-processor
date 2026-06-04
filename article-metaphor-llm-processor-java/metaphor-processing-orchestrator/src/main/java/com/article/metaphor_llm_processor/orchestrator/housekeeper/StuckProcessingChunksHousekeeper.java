package com.article.metaphor_llm_processor.orchestrator.housekeeper;

import com.article.metaphor_llm_processor.common.model.DocumentChunkState;
import com.article.metaphor_llm_processor.common.model.IndexedDocumentChunk;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.orchestrator.configproperties.ProcessingConfigProperties;
import com.article.metaphor_llm_processor.orchestrator.model.ChunkProcessingError;
import com.article.metaphor_llm_processor.orchestrator.model.ChunkProcessingState;
import com.article.metaphor_llm_processor.orchestrator.repository.ChunkProcessingStateRepository;
import com.article.metaphor_llm_processor.orchestrator.statemanager.StateManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class StuckProcessingChunksHousekeeper {

    private static final String PROCESSING_ATTEMPT_TIMEOUT_MESSAGE = "Chunk is too long in the same processing state";

    private final IndexedDocumentChunkRepository chunkRepository;
    private final ChunkProcessingStateRepository chunkProcessingStateRepository;
    private final StateManager stateManager;
    private final int maxProcessingRetries;

    public StuckProcessingChunksHousekeeper(IndexedDocumentChunkRepository chunkRepository,
                                            ChunkProcessingStateRepository chunkProcessingStateRepository,
                                            StateManager stateManager,
                                            ProcessingConfigProperties processingConfigProperties) {
        this.chunkRepository = chunkRepository;
        this.chunkProcessingStateRepository = chunkProcessingStateRepository;
        this.stateManager = stateManager;
        this.maxProcessingRetries = processingConfigProperties.maxRetry();
    }

    @Scheduled(fixedDelayString = "#{@'processing-com.metaphor.llm.processor.configproperties.HousekeepingConfigProperties'.intervalInMillis}")
    @Transactional
    public void run() {
        log.info("Housekeeping of stuck chunks in progress...");

        List<IndexedDocumentChunk> chunks = chunkRepository.findStuckChunksInProcessing(
                100, 100 // TODO
        );

        Instant now = Instant.now();
        for (IndexedDocumentChunk chunk : chunks) {
            log.info("Chunk[id = {}] is about to be processed", chunk.getId());

            ChunkProcessingState chunkProcessingState = chunkProcessingStateRepository.findByChunkId(
                    chunk.getId()
            ).orElse(ChunkProcessingState.builder()
                    .chunkId(chunk.getId())
                    .build());

            if (chunkProcessingState.getErrors().size() < maxProcessingRetries) {
                ChunkProcessingError error = new ChunkProcessingError(
                        PROCESSING_ATTEMPT_TIMEOUT_MESSAGE,
                        now,
                        chunkProcessingState.getReachedMilestone()
                );
                chunkProcessingState.addError(error);
                chunkProcessingState.setFailedOnLastExecution(true);
                chunkProcessingState.setLastExecutionTimestamp(now);
            }

            if (chunkProcessingState.getErrors().size() >= maxProcessingRetries) {
                chunk.setState(DocumentChunkState.FAILED);
                chunkProcessingState.deactivate();
                stateManager.updateDocumentIfAllChunksProcessed(chunk.getId(), chunk.getDocumentId());
            } else {
                chunk.setState(DocumentChunkState.REPROCESSING_NEEDED);
            }

            chunkProcessingStateRepository.save(chunkProcessingState);
        }

        chunkRepository.saveAll(chunks);
        log.info("Stuck chunk housekeeping is complete");
    }
}