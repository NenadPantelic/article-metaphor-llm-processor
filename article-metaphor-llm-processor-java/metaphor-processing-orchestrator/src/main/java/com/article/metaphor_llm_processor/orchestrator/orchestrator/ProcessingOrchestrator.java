package com.article.metaphor_llm_processor.orchestrator.orchestrator;

import com.article.metaphor_llm_processor.common.dto.processing.ChunkProcessingData;
import com.article.metaphor_llm_processor.common.model.*;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentRepository;
import com.article.metaphor_llm_processor.orchestrator.configproperties.ProcessingConfigProperties;
import com.article.metaphor_llm_processor.orchestrator.exception.ProcessorException;
import com.article.metaphor_llm_processor.orchestrator.producer.ChunkProcessingMessageProducer;
import com.article.metaphor_llm_processor.orchestrator.statemanager.StateManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public abstract class ProcessingOrchestrator {

    protected final ChunkProcessingMessageProducer chunkProcessingMessageProducer;
    protected final StateManager stateManager;
    protected final IndexedDocumentRepository documentRepository;
    protected final IndexedDocumentChunkRepository chunkRepository;
    private final ProcessingConfigProperties processingConfigProperties;

    public ProcessingOrchestrator(
            IndexedDocumentRepository documentRepository,
            IndexedDocumentChunkRepository chunkRepository,
            ChunkProcessingMessageProducer chunkProcessingMessageProducer,
            StateManager stateManager,
            ProcessingConfigProperties processingConfigProperties) {
        this.chunkProcessingMessageProducer = chunkProcessingMessageProducer;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.stateManager = stateManager;
        this.processingConfigProperties = processingConfigProperties;
    }

    protected void doProcess(IndexedDocumentChunk chunk,
                             DocumentChunkState newState) {
        String documentId = chunk.getDocumentId();
        String chunkId = chunk.getId();

        try {
            String routingKey = getRoutingKey(newState);
            if (routingKey == null) {
                throw new ProcessorException(
                        String.format("State cannot be mapped to a valid routing key: %s", newState)
                );
            }

            chunk.setState(newState);
            ChunkProcessingData chunkProcessingData = createChunkProcessingData(chunk);
            chunkProcessingMessageProducer.sendMessage(routingKey, chunkProcessingData);
            log.info("Successfully sent chunk[id = {}, documentId = {}] to processing. The first step is the lexical" +
                    " unit processing", chunk.getId(), chunk.getDocumentId()
            );
        } catch (Exception e) {
            log.warn("The document[id = {}] processing failed. Reason: {}", documentId, e.getMessage(), e);
            Instant now = Instant.now();
            chunk.addFailedAttempt(
                    new ChunkProcessingAttempt(now, e.getMessage(), chunk.getMilestone())
            );
            chunk.setLastProcessingAttemptedAt(now);
            // not possible to retry, all attempts exhausted
            if (chunk.getFailedAttempts().size() >= processingConfigProperties.maxRetry()) {
                log.warn("Processing attempt exhausted for chunk[id = {}, documentId = {}]",
                        chunk.getId(), chunk.getDocumentId()
                );
                chunk.setState(DocumentChunkState.PROCESSING_FAILED);
                chunkRepository.save(chunk);
                stateManager.updateDocumentIfAllChunksProcessed(chunkId, documentId);
            } else {
                chunk.setLastAttemptReprocessable(true);
                chunkRepository.save(chunk);
            }
        }
    }

    abstract ChunkProcessingData createChunkProcessingData(IndexedDocumentChunk chunk);

    private String getRoutingKey(DocumentChunkState status) {
        return switch (status) {
            case PENDING, PENDING_REPROCESSING, STARTED_PROCESSING, LEXICAL_UNIT_PROCESSING__PENDING ->
                    processingConfigProperties.lexicalUnitProcessingExchange();
            case DICTIONARY_ACCESS__PENDING -> processingConfigProperties.lemmaMeaningLookupProcessingExchange();
            case METAPHOR_ANALYSIS__PENDING -> processingConfigProperties.metaphorAnalysisProcessingExchange();
            default -> null;
        };
    }
}
