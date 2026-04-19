package com.article.metaphor_llm_processor.orchestrator.orchestrator;

import com.article.metaphor_llm_processor.common.dto.processing.ChunkProcessingData;
import com.article.metaphor_llm_processor.common.dto.processing.in.DocumentChunk;
import com.article.metaphor_llm_processor.common.model.DocumentChunkStatus;
import com.article.metaphor_llm_processor.common.model.DocumentStatus;
import com.article.metaphor_llm_processor.common.model.IndexedDocument;
import com.article.metaphor_llm_processor.common.model.IndexedDocumentChunk;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentRepository;
import com.article.metaphor_llm_processor.orchestrator.configproperties.ProcessingConfigProperties;
import com.article.metaphor_llm_processor.orchestrator.producer.ChunkProcessingMessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class MetaphorProcessingOrchestrator extends ProcessingOrchestrator {

    private static final Map<DocumentStatus, DocumentStatus> DOCUMENT_STATUS_TRANSITION_MAP = Map.of(
            DocumentStatus.PENDING, DocumentStatus.PROCESSING,
            DocumentStatus.PENDING_REPROCESSING, DocumentStatus.REPROCESSING
    );

    private final IndexedDocumentRepository documentRepository;
    private final IndexedDocumentChunkRepository chunkRepository;

    public MetaphorProcessingOrchestrator(IndexedDocumentRepository documentRepository,
                                          IndexedDocumentChunkRepository chunkRepository,
                                          ChunkProcessingMessageProducer chunkProcessingMessageProducer,
                                          ProcessingConfigProperties processingConfigProperties) {
        super(documentRepository, chunkRepository, chunkProcessingMessageProducer, processingConfigProperties);
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
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

        Optional<IndexedDocumentChunk> chunkOptional = chunkRepository.findFirstChunkEligibleForProcessingByDocumentId(
                document.getId()
        );
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
        doProcess(chunkToProcess, DocumentChunkStatus.STARTED_PROCESSING);
    }

    @Override
    ChunkProcessingData createChunkProcessingData(IndexedDocumentChunk chunk) {
        return new DocumentChunk(chunk.getDocumentId(), chunk.getId(), chunk.getText());
    }


    void updateDocumentStatusIfNeeded(IndexedDocument document) {
        DocumentStatus newStatus = DOCUMENT_STATUS_TRANSITION_MAP.get(document.getStatus());
        if (newStatus != null) {
            document.setStatus(newStatus);
        }
    }
}
