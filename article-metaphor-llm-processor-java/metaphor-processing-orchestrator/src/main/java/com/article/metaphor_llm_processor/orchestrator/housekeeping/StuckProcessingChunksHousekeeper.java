package com.article.metaphor_llm_processor.orchestrator.housekeeping;

import com.article.metaphor_llm_processor.common.model.ChunkProcessingAttempt;
import com.article.metaphor_llm_processor.common.model.DocumentChunkStatus;
import com.article.metaphor_llm_processor.common.model.IndexedDocumentChunk;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.orchestrator.configproperties.ProcessingConfigProperties;
import com.article.metaphor_llm_processor.orchestrator.statemanager.StateManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class StuckProcessingChunksHousekeeper {

    private static final String PROCESSING_ATTEMPT_TIMEOUT_MESSAGE = "Chunk is too long in the same processing state";

    private final IndexedDocumentChunkRepository chunkRepository;
    private final StateManager stateManager;
    private final int maxProcessingRetries;

    public StuckProcessingChunksHousekeeper(IndexedDocumentChunkRepository chunkRepository,
                                            StateManager stateManager,
                                            ProcessingConfigProperties processingConfigProperties) {
        this.chunkRepository = chunkRepository;
        this.stateManager = stateManager;
        this.maxProcessingRetries = processingConfigProperties.maxRetry();
    }

    @Scheduled(fixedDelayString = "#{@'processing-com.metaphor.llm.processor.configproperties.HousekeepingConfigProperties'.intervalInMillis}")
    @Transactional
    public void run() {
        log.info("Housekeeping in progress...");

        List<IndexedDocumentChunk> chunks = chunkRepository.findStuckChunksInProcessing(
                100, 100
        );

        Instant now = Instant.now();
        for (IndexedDocumentChunk chunk : chunks) {
            log.info("Chunk[id = {}] is about to be processed", chunk.getId());

            ChunkProcessingAttempt chunkProcessingAttempt = new ChunkProcessingAttempt(
                    now, PROCESSING_ATTEMPT_TIMEOUT_MESSAGE, chunk.getMilestone()
            );
            chunk.addAttempt(chunkProcessingAttempt);

            if (chunk.getAttempts().size() >= maxProcessingRetries) {
                chunk.setStatus(DocumentChunkStatus.PROCESSING_FAILED);
                chunk.setLastAttemptReprocessable(false);
                stateManager.updateDocumentIfAllChunksProcessed(chunk.getId(), chunk.getDocumentId());
            } else {
                chunk.setStatus(DocumentChunkStatus.ANOTHER_ATTEMPT_NEEDED);
                chunk.setLastAttemptReprocessable(true);
            }
        }

        chunkRepository.saveAll(chunks);
        log.info("Housekeeping is complete");
    }
}
