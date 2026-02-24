package com.article.metaphor_llm_processor.orchestrator.orchestrator;

import com.article.metaphor_llm_processor.common.model.DocumentChunkStatus;
import com.article.metaphor_llm_processor.common.model.DocumentStatus;
import com.article.metaphor_llm_processor.common.model.IndexedDocument;
import com.article.metaphor_llm_processor.common.model.IndexedDocumentChunk;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentRepository;
import com.article.metaphor_llm_processor.orchestrator.configproperties.ProcessingConfigProperties;
import com.article.metaphor_llm_processor.orchestrator.dto.message.DocumentChunkProcessingMessage;
import com.article.metaphor_llm_processor.orchestrator.model.ChunkProcessingAttempt;
import com.article.metaphor_llm_processor.orchestrator.producer.ChunkProcessingMessageProducer;
import com.article.metaphor_llm_processor.orchestrator.repository.DocumentReprocessingRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class MetaphorProcessingOrchestrator {

    private static final Map<DocumentStatus, DocumentStatus> DOCUMENT_STATUS_TRANSITION_MAP = Map.of(
            DocumentStatus.PENDING, DocumentStatus.PROCESSING,
            DocumentStatus.PENDING_REPROCESSING, DocumentStatus.REPROCESSING
    );

    private final IndexedDocumentRepository documentRepository;
    private final IndexedDocumentChunkRepository chunkRepository;
    private final DocumentReprocessingRequestRepository documentReprocessingRequestRepository;
    private final ChunkProcessingMessageProducer chunkProcessingMessageProducer;
    private final int maxProcessingRetries;

    public MetaphorProcessingOrchestrator(IndexedDocumentRepository documentRepository,
                                          IndexedDocumentChunkRepository chunkRepository,
                                          DocumentReprocessingRequestRepository documentReprocessingRequestRepository,
                                          ChunkProcessingMessageProducer chunkProcessingMessageProducer,
                                          ProcessingConfigProperties processingConfigProperties) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.documentReprocessingRequestRepository = documentReprocessingRequestRepository;
        this.chunkProcessingMessageProducer = chunkProcessingMessageProducer;
        this.maxProcessingRetries = processingConfigProperties.maxRetry();
    }

    @Scheduled(fixedDelayString = "#{@'processing-com.metaphor.llm.processor.configproperties.ProcessingConfigProperties'.intervalInMillis}")
    public void process() {
        log.info("Processing the next document/chunk...");

        // NOTE: document reprocessing is expected to happen after the initial processing
        Optional<IndexedDocument> documentOptional = documentRepository.findOldestEligibleDocumentForProcessing();
        if (documentOptional.isEmpty()) {
            log.warn("There is no document that is ready for processing...");
            return;
        }

        var document = documentOptional.get();
        log.info("Document[id = {}, status = {}] is chosen. Its next chunk is about to be processed.",
                document.getId(), document.getStatus()
        );
        updateDocumentStatusIfNeeded(document);
        documentRepository.save(document);

        Optional<IndexedDocumentChunk> chunkOptional = chunkRepository.findFirstChunkEligibleForProcessing(document.getId());
        if (chunkOptional.isEmpty()) {
            log.info("There is no chunk waiting to be processed...");
            document.setStatus(DocumentStatus.INCOMPLETE); // should not happen
            documentRepository.save(document);
            return;
        }

        IndexedDocumentChunk chunkToProcess = chunkOptional.get();
        String chunkId = chunkToProcess.getId();
        String chunkDocumentId = chunkToProcess.getDocumentId();
        log.info("Chunk[id = {}, documentId = {}] is about to be processed.", chunkId, chunkDocumentId);
        var now = Instant.now();

        try {
            chunkToProcess.setStatus(DocumentChunkStatus.LEXICAL_UNIT_PROCESSING__PENDING);
            chunkToProcess = chunkRepository.save(chunkToProcess);

            chunkProcessingMessageProducer.sendMessage(new DocumentChunkProcessingMessage(chunkToProcess.getId()));
            log.info("Successfully sent chunk[id = {}, documentId = {}] to processing. The first step is the lexical" +
                    " unit processing", chunkId, chunkDocumentId);
        } catch (Exception e) {
            log.warn("The document[id = {}] processing failed. Reason: {}", document.getId(), e.getMessage(), e);
            chunkToProcess.addAttempt(
                    new ChunkProcessingAttempt(
                            now, e.getMessage(), DocumentChunkStatus.LEXICAL_UNIT_PROCESSING__PENDING
                    )
            );
            chunkToProcess.setLastProcessingAttemptedAt(now);
            // not possible to retry, all attempts exhausted
            if (chunkToProcess.getAttempts().size() >= maxProcessingRetries) {
                log.warn("Processing attempt exhausted for chunk[id = {}, documentId = {}]", chunkId, chunkDocumentId);
                chunkToProcess.setStatus(DocumentChunkStatus.FAILED_TO_PROCESS);
                chunkRepository.save(chunkToProcess);
                updateDocumentIfAllChunksProcessed(chunkId, chunkDocumentId, document);
            } else {
                chunkToProcess.setStatus(DocumentChunkStatus.NEXT_ATTEMPT_NEEDED);
                chunkRepository.save(chunkToProcess);
            }
        }
    }

    void updateDocumentStatusIfNeeded(IndexedDocument document) {
        DocumentStatus newStatus = DOCUMENT_STATUS_TRANSITION_MAP.get(document.getStatus());
        if (newStatus != null) {
            document.setStatus(newStatus);
        }
    }

    void updateDocumentIfAllChunksProcessed(String chunkId, String chunkDocumentId, IndexedDocument document) {
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
                tryRemoveReprocessingRequest(document.getId());
            }
        }
    }

    void tryRemoveReprocessingRequest(String documentId) {
        try {
            documentReprocessingRequestRepository.deleteByDocumentId(documentId);
        } catch (Exception e) {
            log.error("Unable to delete the document reprocessing request[documentId = {}]. This will block the next " +
                    "such request for the same document.", documentId, e);
        }
    }

}
