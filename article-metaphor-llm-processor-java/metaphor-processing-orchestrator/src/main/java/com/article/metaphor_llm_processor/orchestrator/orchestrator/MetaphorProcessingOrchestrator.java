package com.article.metaphor_llm_processor.orchestrator.orchestrator;

import com.article.metaphor_llm_processor.common.model.DocumentChunkState;
import com.article.metaphor_llm_processor.common.model.DocumentState;
import com.article.metaphor_llm_processor.common.model.IndexedDocument;
import com.article.metaphor_llm_processor.common.model.IndexedDocumentChunk;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentRepository;
import com.article.metaphor_llm_processor.orchestrator.configproperties.ProcessingConfigProperties;
import com.article.metaphor_llm_processor.orchestrator.dto.ProcessingMessage;
import com.article.metaphor_llm_processor.orchestrator.producer.ChunkProcessingMessageProducer;
import com.article.metaphor_llm_processor.orchestrator.repository.ChunkProcessingStateRepository;
import com.article.metaphor_llm_processor.orchestrator.statemanager.StateManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class MetaphorProcessingOrchestrator extends ProcessingOrchestrator {

    private static final Map<DocumentState, DocumentState> DOCUMENT_STATUS_TRANSITION_MAP = Map.of(
            DocumentState.PENDING_PROCESSING, DocumentState.PROCESSING,
            DocumentState.PENDING_REPROCESSING, DocumentState.REPROCESSING
    );


    public MetaphorProcessingOrchestrator(IndexedDocumentRepository documentRepository,
                                          IndexedDocumentChunkRepository chunkRepository,
                                          StateManager stateManager,
                                          ChunkProcessingMessageProducer chunkProcessingMessageProducer,
                                          ChunkProcessingStateRepository chunkProcessingStateRepository,
                                          ProcessingConfigProperties processingConfigProperties) {
        super(documentRepository,
                chunkRepository,
                chunkProcessingMessageProducer,
                stateManager,
                chunkProcessingStateRepository,
                processingConfigProperties
        );
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
                document.getId(), document.getState()
        );
        updateDocumentStateIfNeeded(document);
        documentRepository.save(document);

        Optional<IndexedDocumentChunk> chunkOptional = chunkRepository.findFirstChunkEligibleForProcessingByDocumentId(
                document.getId()
        );
        if (chunkOptional.isEmpty()) {
            log.info("There is no chunk waiting to be processed...");
            document.setState(DocumentState.PROCESSED_INCOMPLETE); // should not happen
            documentRepository.save(document);
            return;
        }

        IndexedDocumentChunk chunkToProcess = chunkOptional.get();
        String chunkId = chunkToProcess.getId();
        String chunkDocumentId = chunkToProcess.getDocumentId();
        log.info("Chunk[id = {}, documentId = {}] is about to be processed.", chunkId, chunkDocumentId);
        doProcess(chunkToProcess,
                DocumentChunkState.PROCESSING,
                new ProcessingMessage(chunkId, chunkToProcess.getText())
        );
    }

    void updateDocumentStateIfNeeded(IndexedDocument document) {
        DocumentState newState = DOCUMENT_STATUS_TRANSITION_MAP.get(document.getState());
        if (newState != null) {
            document.setState(newState);
        }
    }
}
