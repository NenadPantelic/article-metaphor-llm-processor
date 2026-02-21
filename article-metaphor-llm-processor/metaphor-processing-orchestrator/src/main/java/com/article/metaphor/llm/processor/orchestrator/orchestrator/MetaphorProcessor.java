package com.article.metaphor.llm.processor.orchestrator.orchestrator;

import com.article.metaphor.llm.processor.common.model.*;
import com.article.metaphor.llm.processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor.llm.processor.common.repository.IndexedDocumentRepository;
import com.article.metaphor.llm.processor.orchestrator.configproperties.ProcessingConfigProperties;
import com.article.metaphor.llm.processor.orchestrator.repository.DocumentReprocessingRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class MetaphorProcessor {

    private static final Map<DocumentStatus, DocumentStatus> DOCUMENT_STATUS_TRANSITION_MAP = Map.of(
            DocumentStatus.PENDING, DocumentStatus.PROCESSING,
            DocumentStatus.PENDING_REPROCESSING, DocumentStatus.REPROCESSING
    );

    private final IndexedDocumentRepository documentRepository;
    private final IndexedDocumentChunkRepository chunkRepository;
    private final DocumentReprocessingRequestRepository documentReprocessingRequestRepository;
    private final int maxProcessingRetries;

    public MetaphorProcessor(IndexedDocumentRepository documentRepository,
                             IndexedDocumentChunkRepository chunkRepository,
                             DocumentReprocessingRequestRepository documentReprocessingRequestRepository,
                             ProcessingConfigProperties processingConfigProperties) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.documentReprocessingRequestRepository = documentReprocessingRequestRepository;
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
            chunkToProcess.setStatus(DocumentChunkStatus.PROCESSING);
            chunkToProcess = chunkRepository.save(chunkToProcess);
            // TODO: send a message to another service, i.e. run the pipeline
//            var metaphors = analyzeMetaphors(document, chunkToProcess);
//            log.info("Document[id = {}] metaphor analysis done. No of found metaphors: {}",
//                    document.getId(), metaphors == null ? 0 : metaphors.size()
//            );
//            document.addMetaphors(metaphors);
//            documentRepository.save(document); // not to keep it in memory if something crashes
//
//            chunkToProcess.setStatus(DocumentChunkStatus.SUCCESSFULLY_PROCESSED);
//            chunkToProcess.setLastProcessingAttemptedAt(now);
//            chunkToProcess.addAttempt(new ChunkProcessingAttempt(now, null));
//
//            chunkRepository.save(chunkToProcess);
//            log.info("Successfully processed chunk[id = {}, documentId = {}]", chunkId, chunkDocumentId);
//            updateDocumentIfAllChunksProcessed(chunkId, chunkDocumentId, document);
        } catch (Exception e) {
            // TODO: LLM can be unavailable or return a result that is not serializable (though this should not happen
            // with newer versions of LLM)
            log.warn("The document[id = {}] processing failed. Reason: {}", document.getId(), e.getMessage(), e);
            chunkToProcess.addAttempt(new ChunkProcessingAttempt(now, e.getMessage()));
            chunkToProcess.setLastProcessingAttemptedAt(now);

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


    // offset returned by LLM is an offset computed in the chunk, but the idea is to store the offset from the beginning
    // of the whole document
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
