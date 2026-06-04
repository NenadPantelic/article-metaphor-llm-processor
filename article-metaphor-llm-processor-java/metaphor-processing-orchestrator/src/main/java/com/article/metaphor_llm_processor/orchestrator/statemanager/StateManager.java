package com.article.metaphor_llm_processor.orchestrator.statemanager;

import com.article.metaphor_llm_processor.common.model.DocumentChunkState;
import com.article.metaphor_llm_processor.common.model.DocumentState;
import com.article.metaphor_llm_processor.common.model.IndexedDocument;
import com.article.metaphor_llm_processor.common.model.IndexedDocumentChunk;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentRepository;
import com.article.metaphor_llm_processor.orchestrator.model.ChunkProcessingError;
import com.article.metaphor_llm_processor.orchestrator.model.ChunkProcessingState;
import com.article.metaphor_llm_processor.orchestrator.repository.ChunkProcessingStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class StateManager {

    private final IndexedDocumentRepository documentRepository;
    private final IndexedDocumentChunkRepository chunkRepository;
    private final ChunkProcessingStateRepository chunkProcessingStateRepository;

    public StateManager(IndexedDocumentRepository documentRepository,
                        IndexedDocumentChunkRepository chunkRepository,
                        ChunkProcessingStateRepository chunkProcessingStateRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.chunkProcessingStateRepository = chunkProcessingStateRepository;
    }

    @Transactional
    public void failChunk(IndexedDocumentChunk chunk,
                          String errorMessage) {
        Instant now = Instant.now();
        ChunkProcessingState chunkProcessingState = chunkProcessingStateRepository.findByChunkId(chunk.getId()).orElse(
                ChunkProcessingState.builder()
                        .chunkId(chunk.getId())
                        .build()
        );

        chunkProcessingState.setFailedOnLastExecution(true);
        chunkProcessingState.setLastExecutionTimestamp(now);
        chunkProcessingState.addError(new ChunkProcessingError(errorMessage, now, null));
        chunkProcessingState.deactivate();
        chunk.setState(DocumentChunkState.FAILED);
        chunkProcessingStateRepository.save(chunkProcessingState);
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
            DocumentState documentState = processingFailuresCount == 0 ?
                    DocumentState.PROCESSED_SUCCESSFULLY :
                    DocumentState.PROCESSED_INCOMPLETE;
            document.setState(documentState);
            documentRepository.save(document);
        }
    }
}
