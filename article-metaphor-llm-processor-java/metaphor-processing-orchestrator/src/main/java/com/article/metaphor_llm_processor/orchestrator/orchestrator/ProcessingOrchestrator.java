package com.article.metaphor_llm_processor.orchestrator.orchestrator;

import com.article.metaphor_llm_processor.common.dto.processing.ChunkProcessingData;
import com.article.metaphor_llm_processor.common.model.*;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentRepository;
import com.article.metaphor_llm_processor.orchestrator.configproperties.ProcessingConfigProperties;
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
    private final int maxProcessingRetries;

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
        this.maxProcessingRetries = processingConfigProperties.maxRetry();
    }

    protected void doProcess(IndexedDocumentChunk chunk,
                             DocumentChunkStatus newStatus) {
        String documentId = chunk.getDocumentId();
        String chunkId = chunk.getId();

        try {
            chunk.setStatus(newStatus);
            String routingKey = getRoutingKey(newStatus);
            ChunkProcessingData chunkProcessingData = createChunkProcessingData(chunk);
            chunkProcessingMessageProducer.sendMessage(routingKey, chunkProcessingData);
            log.info("Successfully sent chunk[id = {}, documentId = {}] to processing. The first step is the lexical" +
                    " unit processing", chunk.getId(), chunk.getDocumentId()
            );
        } catch (Exception e) {
            log.warn("The document[id = {}] processing failed. Reason: {}", documentId, e.getMessage(), e);
            Instant now = Instant.now();
            chunk.addAttempt(
                    new ChunkProcessingAttempt(now, e.getMessage(), chunk.getMilestone())
            );
            chunk.setLastProcessingAttemptedAt(now);
            // not possible to retry, all attempts exhausted
            if (chunk.getAttempts().size() >= maxProcessingRetries) {
                log.warn("Processing attempt exhausted for chunk[id = {}, documentId = {}]",
                        chunk.getId(), chunk.getDocumentId()
                );
                chunk.setStatus(DocumentChunkStatus.PROCESSING_FAILED);
                chunkRepository.save(chunk);
                stateManager.updateDocumentIfAllChunksProcessed(chunkId, documentId);
            } else {
                chunk.setLastAttemptReprocessable(true);
                chunkRepository.save(chunk);
            }
        }
    }

    abstract ChunkProcessingData createChunkProcessingData(IndexedDocumentChunk chunk);

    private String getRoutingKey(DocumentChunkStatus status) {
        return switch (status) {
            // TODO
            case PENDING, PENDING_REPROCESSING, STARTED_PROCESSING, LEXICAL_UNIT_PROCESSING__PENDING -> "1";
            case DICTIONARY_ACCESS__PENDING -> "2";
            case METAPHOR_ANALYSIS__PENDING -> "3";
            default -> "";
        };
    }
}
