package com.article.metaphor_llm_processor.api.service.impl;

import com.article.metaphor_llm_processor.common.model.DocumentChunkState;
import com.article.metaphor_llm_processor.common.model.DocumentState;
import com.article.metaphor_llm_processor.common.model.IndexedDocument;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentRepository;
import com.article.metaphor_llm_processor.api.service.MetaphorReprocessingService;
import com.article.metaphor_llm_processor.api.dto.request.MetaphorReprocessingRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class MetaphorReprocessingServiceImpl implements MetaphorReprocessingService {

    private static final List<DocumentState> FINAL_STATUSES = List.of(DocumentState.DONE, DocumentState.INCOMPLETE);
    private final IndexedDocumentRepository documentRepository;
    private final IndexedDocumentChunkRepository chunkRepository;

    public MetaphorReprocessingServiceImpl(IndexedDocumentRepository documentRepository,
                                           IndexedDocumentChunkRepository chunkRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    public void handleReprocessingRequest(MetaphorReprocessingRequest metaphorReprocessingRequest) {
        String documentId = metaphorReprocessingRequest.documentId();
        List<String> reasons = metaphorReprocessingRequest.reasons();
        log.info("Processing the document[id = {}]. Reasons: {}", documentId, reasons);

        Optional<IndexedDocument> documentOptional = documentRepository.findById(documentId);
        if (documentOptional.isEmpty()) {
            log.error("There is no document with id {} to be processed...", documentId);
            return;
        }

        var document = documentOptional.get();
        if (!FINAL_STATUSES.contains(document.getState())) {
            log.error("Document[id = {}] is not in final state, hence cannot be processed again.", documentId);
            return;
        }

        document.setState(DocumentState.PENDING_REPROCESSING);
        document.clearAllMetaphors();
        documentRepository.save(document);

        var chunks = chunkRepository.findByDocumentId(documentId);
        chunks.forEach(chunk -> {
            chunk.clearAllFailedAttempts();
            chunk.setState(DocumentChunkState.PENDING_REPROCESSING);
        });
        chunkRepository.saveAll(chunks);
    }
}