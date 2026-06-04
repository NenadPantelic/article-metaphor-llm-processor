package com.article.metaphor_llm_processor.orchestrator.orchestrator;

import com.article.metaphor_llm_processor.common.model.DocumentChunkState;
import com.article.metaphor_llm_processor.common.model.DocumentState;
import com.article.metaphor_llm_processor.common.model.IndexedDocument;
import com.article.metaphor_llm_processor.common.model.IndexedDocumentChunk;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentRepository;
import com.article.metaphor_llm_processor.orchestrator.configproperties.ProcessingConfigProperties;
import com.article.metaphor_llm_processor.orchestrator.dto.ReprocessingMessage;
import com.article.metaphor_llm_processor.orchestrator.dto.ReprocessingType;
import com.article.metaphor_llm_processor.orchestrator.producer.ChunkProcessingMessageProducer;
import com.article.metaphor_llm_processor.orchestrator.repository.ChunkProcessingStateRepository;
import com.article.metaphor_llm_processor.orchestrator.statemanager.StateManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class MetaphorReprocessingOrchestrator extends ProcessingOrchestrator {

    public MetaphorReprocessingOrchestrator(IndexedDocumentRepository documentRepository,
                                            IndexedDocumentChunkRepository chunkRepository,
                                            ChunkProcessingMessageProducer chunkProcessingMessageProducer,
                                            StateManager stateManager,
                                            ChunkProcessingStateRepository chunkProcessingStateRepository,
                                            ProcessingConfigProperties processingConfigProperties) {
        super(
                documentRepository,
                chunkRepository,
                chunkProcessingMessageProducer,
                stateManager,
                chunkProcessingStateRepository,
                processingConfigProperties);
    }

    @Scheduled(fixedDelayString = "#{@'processing-com.metaphor.llm.processor.configproperties.ProcessingConfigProperties'.reprocessingIntervalInMillis}")
    public void process() {
        log.info("Fetching the next chunk for reprocessing...");

        // first try with documents whose reprocessing has been requested by the user
        Optional<IndexedDocument> documentOptional = documentRepository.findOldestEligibleDocumentForReprocessing();
        if (documentOptional.isPresent()) {
            reprocessChunkByRequestedDocument(documentOptional.get());
        } else { // get chunk which processing initially failed due to an intermittent issue
            Optional<IndexedDocumentChunk> chunkOptional = chunkRepository.findFirstChunkEligibleForReprocessing();
            if (chunkOptional.isEmpty()) {
                log.info("There is no chunk waiting to be processed...");
                return;
            }

            reprocessChunkAfterFailure(chunkOptional.get());
        }
    }

    private void reprocessChunkByRequestedDocument(IndexedDocument document) {
        log.info("Document[id = {}, status = {}] is chosen. Its next chunk is about to be processed.",
                document.getId(), document.getState()
        );

        Optional<IndexedDocumentChunk> chunkOptional = chunkRepository.findFirstChunkEligibleForProcessingByDocumentId(
                document.getId()
        );
        if (chunkOptional.isEmpty()) {
            log.info("There is no chunk associated with document {}", document.getId());
            document.setState(DocumentState.PROCESSED_SUCCESSFULLY);
            documentRepository.save(document);
            return;
        }

        if (document.getState() != DocumentState.REPROCESSING) {
            document.setState(DocumentState.REPROCESSING);
            documentRepository.save(document);
        }

        reprocessChunk(chunkOptional.get(), ReprocessingType.USER_REQUESTED);
    }

    private void reprocessChunkAfterFailure(IndexedDocumentChunk chunk) {
        DocumentChunkState currentState = chunk.getState();
        log.info("Chunk to be reprocessed[id = {}, current status = {}]",
                chunk.getId(), currentState
        );

        Optional<IndexedDocument> documentOptional = documentRepository.findById(chunk.getDocumentId());
        if (documentOptional.isEmpty()) {
            stateManager.failChunk(chunk, "Chunk document not found");
        } else {
            reprocessChunk(chunk, ReprocessingType.ATTEMPT_AFTER_FAILURE);
        }
    }

    private void reprocessChunk(IndexedDocumentChunk chunk, ReprocessingType reprocessingType) {
        try {
            log.info("Reprocess chunk: chunkId = {}, reprocessingType = {} ", chunk.getId(), reprocessingType);
            doProcess(chunk, DocumentChunkState.REPROCESSING, new ReprocessingMessage(chunk.getId(), reprocessingType));
        } catch (Exception e) {
            log.error("Reprocessing failed due to {}", e.getMessage(), e);
            stateManager.failChunk(chunk, e.getMessage());
        }
    }
}
