package com.article.metaphor_llm_processor.orchestrator.orchestrator;

import com.article.metaphor_llm_processor.common.dto.processing.ChunkProcessingData;
import com.article.metaphor_llm_processor.common.model.*;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentRepository;
import com.article.metaphor_llm_processor.orchestrator.configproperties.ProcessingConfigProperties;
import com.article.metaphor_llm_processor.orchestrator.producer.ChunkProcessingMessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public abstract class ProcessingOrchestrator {

    private final ChunkProcessingMessageProducer chunkProcessingMessageProducer;
    private final IndexedDocumentRepository documentRepository;
    private final IndexedDocumentChunkRepository chunkRepository;
    private final int maxProcessingRetries;

    public ProcessingOrchestrator(
            IndexedDocumentRepository documentRepository,
            IndexedDocumentChunkRepository chunkRepository,
            ChunkProcessingMessageProducer chunkProcessingMessageProducer,
            ProcessingConfigProperties processingConfigProperties) {
        this.chunkProcessingMessageProducer = chunkProcessingMessageProducer;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
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
                documentRepository.findById(chunk.getDocumentId()).ifPresent(
                        (document) -> updateDocumentIfAllChunksProcessed(chunkId, documentId, document)
                );
            } else {
                chunk.setShouldBeReprocessed(true);
                chunkRepository.save(chunk);
            }
        }
    }

    abstract ChunkProcessingData createChunkProcessingData(IndexedDocumentChunk chunk);

    protected void failChunkAndSucceedingChunks(IndexedDocumentChunk startingChunk,
                                                String errorMessage) {
        List<IndexedDocumentChunk> chunks = chunkRepository.findByDocumentIdAndOrderGreaterThanOrEq(
                startingChunk.getDocumentId(), startingChunk.getOrder()
        );
        Instant now = Instant.now();

        chunks.forEach(chunk -> {
            chunk.setLastProcessingAttemptedAt(now);
            chunk.setStatus(DocumentChunkStatus.PROCESSING_FAILED);
            chunk.addAttempt(
                    new ChunkProcessingAttempt(now, errorMessage, chunk.getMilestone())
            );
        });

        chunkRepository.saveAll(chunks);
    }

    protected void updateDocumentIfAllChunksProcessed(String chunkId,
                                                      String chunkDocumentId,
                                                      IndexedDocument document) {
        log.info("Checking if chunkId '{}' was the last chunk of document[id = {}]", chunkId, chunkDocumentId);
        int allChunksCount = chunkRepository.countByDocumentId(chunkDocumentId);
        // TODO: can be one aggregating query
        int successfullyProcessedCount = chunkRepository.countSuccessfullyProcessedByDocumentId(chunkDocumentId);
        int processingFailuresCount = chunkRepository.countProcessingFailuresByDocumentId(chunkDocumentId);

        log.info("Document[id = {}] chunk processing completeness report: processed with success = {}, " +
                        "processed with failure = {}, total = {}", chunkDocumentId, successfullyProcessedCount,
                processingFailuresCount, allChunksCount);

        if (successfullyProcessedCount + processingFailuresCount == allChunksCount) {
            var currentStatus = document.getStatus();
            log.info("All chunks of a document[id = {}] are processed.", chunkDocumentId);
            DocumentStatus documentStatus = processingFailuresCount == 0 ?
                    DocumentStatus.DONE :
                    DocumentStatus.INCOMPLETE;
            document.setStatus(documentStatus);
            documentRepository.save(document);

            if (currentStatus == DocumentStatus.REPROCESSING) {
                // tryRemoveReprocessingRequest(document.getId());
            }
        }
    }

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
