package com.article.metaphor_llm_processor.orchestrator.orchestrator;

import com.article.metaphor_llm_processor.common.model.DocumentChunkState;
import com.article.metaphor_llm_processor.common.model.IndexedDocumentChunk;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentRepository;
import com.article.metaphor_llm_processor.orchestrator.configproperties.ProcessingConfigProperties;
import com.article.metaphor_llm_processor.orchestrator.dto.PipelineMessage;
import com.article.metaphor_llm_processor.orchestrator.model.ChunkProcessingError;
import com.article.metaphor_llm_processor.orchestrator.model.ChunkProcessingState;
import com.article.metaphor_llm_processor.orchestrator.producer.ChunkProcessingMessageProducer;
import com.article.metaphor_llm_processor.orchestrator.repository.ChunkProcessingStateRepository;
import com.article.metaphor_llm_processor.orchestrator.statemanager.StateManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class ProcessingOrchestrator {

    protected final ChunkProcessingMessageProducer chunkProcessingMessageProducer;
    protected final StateManager stateManager;
    protected final IndexedDocumentRepository documentRepository;
    protected final IndexedDocumentChunkRepository chunkRepository;
    protected final ChunkProcessingStateRepository chunkProcessingStateRepository;
    private final ProcessingConfigProperties processingConfigProperties;

    public ProcessingOrchestrator(
            IndexedDocumentRepository documentRepository,
            IndexedDocumentChunkRepository chunkRepository,
            ChunkProcessingMessageProducer chunkProcessingMessageProducer,
            StateManager stateManager,
            ChunkProcessingStateRepository chunkProcessingStateRepository,
            ProcessingConfigProperties processingConfigProperties) {
        this.chunkProcessingMessageProducer = chunkProcessingMessageProducer;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.stateManager = stateManager;
        this.chunkProcessingStateRepository = chunkProcessingStateRepository;
        this.processingConfigProperties = processingConfigProperties;
    }

    protected void doProcess(IndexedDocumentChunk chunk,
                             DocumentChunkState newState,
                             PipelineMessage pipelineMessage) {
        String documentId = chunk.getDocumentId();
        String chunkId = chunk.getId();

        try {
            chunk.setState(newState);
            chunk.setProcessingStartedAt(Instant.now());
            chunkProcessingMessageProducer.sendMessage(pipelineMessage);
            log.info("Successfully sent chunk[id = {}, documentId = {}] to processing. The first step is the lexical" +
                    " unit processing", chunk.getId(), chunk.getDocumentId()
            );
        } catch (Exception e) {
            log.warn("The document[id = {}] processing failed. Reason: {}", documentId, e.getMessage(), e);
            Instant now = Instant.now();

            ChunkProcessingState chunkProcessingState = chunkProcessingStateRepository.findByChunkId(chunkId).orElse(
                    ChunkProcessingState.builder()
                            .chunkId(chunkId)
                            .build()
            );

            ChunkProcessingError error = new ChunkProcessingError(
                    e.getMessage(),
                    now,
                    null // TODO
            );

            chunkProcessingState.addError(error);
            chunkProcessingState.setFailedOnLastExecution(true);
            chunkProcessingState.setLastExecutionTimestamp(now);
            // not possible to retry, all attempts exhausted
            if (chunkProcessingState.getErrors().size() >= processingConfigProperties.maxRetry()) {
                log.warn("Processing attempt exhausted for chunk[id = {}, documentId = {}]",
                        chunk.getId(), chunk.getDocumentId()
                );
                chunk.setState(DocumentChunkState.FAILED);
                chunkRepository.save(chunk);
                stateManager.updateDocumentIfAllChunksProcessed(chunkId, documentId);
                chunkProcessingState.deactivate();
            } else {
                chunk.setState(DocumentChunkState.REPROCESSING_NEEDED);
                chunkRepository.save(chunk);
            }
        }
    }
}
