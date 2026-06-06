package com.article.metaphor_llm_processor.orchestrator.housekeeper;

import com.article.metaphor_llm_processor.common.model.DocumentChunkState;
import com.article.metaphor_llm_processor.common.model.IndexedDocumentChunk;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.orchestrator.configproperties.ProcessingConfigProperties;
import com.article.metaphor_llm_processor.orchestrator.model.ChunkProcessingState;
import com.article.metaphor_llm_processor.orchestrator.repository.ChunkProcessingStateRepository;
import com.article.metaphor_llm_processor.orchestrator.statemanager.StateManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class FailedProcessingChunksHousekeeper {

    private final IndexedDocumentChunkRepository chunkRepository;
    private final ChunkProcessingStateRepository chunkProcessingStateRepository;
    private final StateManager stateManager;
    private final int maxProcessingRetries;

    public FailedProcessingChunksHousekeeper(IndexedDocumentChunkRepository chunkRepository,
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
        log.info("Housekeeping of failed chunks is in progress...");

        List<ChunkProcessingState> chunkProcessingStates = chunkProcessingStateRepository.findActiveStatesWithTooManyFailures(
                maxProcessingRetries, 100 // TODO
        );

        for (ChunkProcessingState chunkProcessingState : chunkProcessingStates) {
            log.info("Chunk state of chunk[id = {}] is about to be processed", chunkProcessingState.getChunkId());
            Optional<IndexedDocumentChunk> indexedDocumentChunkOptional = chunkRepository.findById(chunkProcessingState.getChunkId());

            chunkProcessingState.deactivate();
            if (indexedDocumentChunkOptional.isPresent()) {
                IndexedDocumentChunk chunk = indexedDocumentChunkOptional.get();
                chunk.setState(DocumentChunkState.FAILED);
                stateManager.updateDocumentIfAllChunksProcessed(chunk.getId(), chunk.getDocumentId());
                chunkRepository.save(chunk);
            }
        }

        chunkProcessingStateRepository.saveAll(chunkProcessingStates);
        log.info("Failed chunk housekeeping is complete");
    }
}