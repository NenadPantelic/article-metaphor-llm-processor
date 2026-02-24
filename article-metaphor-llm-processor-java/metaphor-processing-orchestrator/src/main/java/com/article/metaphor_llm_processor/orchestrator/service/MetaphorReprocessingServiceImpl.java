package com.article.metaphor_llm_processor.orchestrator.service;

import com.metaphor.llm.processor.dto.metaphor.MetaphorReprocessingRequest;
import com.metaphor.llm.processor.model.DocumentChunkStatus;
import com.metaphor.llm.processor.model.DocumentReprocessingRequest;
import com.metaphor.llm.processor.model.DocumentStatus;
import com.metaphor.llm.processor.model.IndexedDocument;
import com.metaphor.llm.processor.repository.DocumentReprocessingRequestRepository;
import com.metaphor.llm.processor.repository.IndexedDocumentChunkRepository;
import com.metaphor.llm.processor.repository.IndexedDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class MetaphorReprocessingServiceImpl implements MetaphorReprocessingService {

    private static final List<DocumentStatus> FINAL_STATUSES = List.of(DocumentStatus.DONE, DocumentStatus.INCOMPLETE);
    private final IndexedDocumentRepository documentRepository;
    private final IndexedDocumentChunkRepository chunkRepository;
    private final DocumentReprocessingRequestRepository documentReprocessingRequestRepository;

    public MetaphorReprocessingServiceImpl(IndexedDocumentRepository documentRepository,
                                           IndexedDocumentChunkRepository chunkRepository,
                                           DocumentReprocessingRequestRepository documentReprocessingRequestRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.documentReprocessingRequestRepository = documentReprocessingRequestRepository;
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
        if (!FINAL_STATUSES.contains(document.getStatus())) {
            log.error("Document[id = {}] is not in final state, hence cannot be processed again.", documentId);
            return;
        }

        var documentReprocessingRequestOptional = tryCreateReprocessingRequest(documentId, reasons);
        if (documentReprocessingRequestOptional.isEmpty()) {
            return;
        }

        document.setStatus(DocumentStatus.PENDING_REPROCESSING);
        document.clearAllMetaphors();
        documentRepository.save(document);

        var chunks = chunkRepository.findByDocumentId(documentId);
        chunks.forEach(chunk -> {
            chunk.clearAllAttempts();
            chunk.setStatus(DocumentChunkStatus.PENDING_REPROCESSING);
        });
        chunkRepository.saveAll(chunks);
    }

    private Optional<DocumentReprocessingRequest> tryCreateReprocessingRequest(String documentId,
                                                                               List<String> reasons) {
        try {
            var reprocessingRequest = DocumentReprocessingRequest.builder()
                    .documentId(documentId)
                    .reasons(reasons)
                    .build();
            reprocessingRequest = documentReprocessingRequestRepository.save(reprocessingRequest);
            return Optional.of(reprocessingRequest);
        } catch (Exception e) {
            log.error("Unable to store the document processing request[documentId = {}, reasons = {}]",
                    documentId, reasons, e);
            return Optional.empty();
        }
    }
}
