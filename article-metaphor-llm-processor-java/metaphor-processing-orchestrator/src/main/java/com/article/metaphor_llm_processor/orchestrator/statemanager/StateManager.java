package com.article.metaphor_llm_processor.orchestrator.statemanager;

import com.article.metaphor_llm_processor.common.model.*;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class StateManager {

    private final IndexedDocumentRepository documentRepository;
    private final IndexedDocumentChunkRepository chunkRepository;

    public StateManager(IndexedDocumentRepository documentRepository, IndexedDocumentChunkRepository chunkRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    public void failChunkAndSucceedingChunks(IndexedDocumentChunk startingChunk, String errorMessage) {
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

    public void failChunk(IndexedDocumentChunk chunk,
                          String errorMessage) {
        Instant now = Instant.now();
        chunk.setLastProcessingAttemptedAt(now);
        chunk.setStatus(DocumentChunkStatus.PROCESSING_FAILED);
        chunk.addAttempt(
                new ChunkProcessingAttempt(now, errorMessage, chunk.getMilestone())
        );
        chunkRepository.save(chunk);
    }

    public void updateDocumentIfAllChunksProcessed(String chunkId,
                                                   String documentId) {
        Optional<IndexedDocument> documentOptional = documentRepository.findById(documentId);
        if (documentOptional.isEmpty()) {
            log.warn("Document[id = {}] not found. Skipping the update.", documentId);
            return;
        }

        IndexedDocument document = documentOptional.get();
        log.info("Checking if chunkId '{}' was the last chunk of document[id = {}]", chunkId, documentId);
        int allChunksCount = chunkRepository.countByDocumentId(documentId);
        // TODO: can be one aggregating query
        int successfullyProcessedCount = chunkRepository.countSuccessfullyProcessedByDocumentId(documentId);
        int processingFailuresCount = chunkRepository.countProcessingFailuresByDocumentId(documentId);

        log.info("Document[id = {}] chunk processing completeness report: processed with success = {}, " +
                        "processed with failure = {}, total = {}", documentId, successfullyProcessedCount,
                processingFailuresCount, allChunksCount);

        if (successfullyProcessedCount + processingFailuresCount == allChunksCount) {
            log.info("All chunks of a document[id = {}] are processed.", documentId);
            DocumentStatus documentStatus = processingFailuresCount == 0 ?
                    DocumentStatus.DONE :
                    DocumentStatus.INCOMPLETE;
            document.setStatus(documentStatus);
            documentRepository.save(document);
        }
    }
}
