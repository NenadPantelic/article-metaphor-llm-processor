package com.article.metaphor_llm_processor.orchestrator.orchestrator;

import com.article.metaphor_llm_processor.common.client.DocumentProcessingStateManagerClient;
import com.article.metaphor_llm_processor.common.dto.processing.ChunkProcessingData;
import com.article.metaphor_llm_processor.common.model.*;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentRepository;
import com.article.metaphor_llm_processor.orchestrator.configproperties.ProcessingConfigProperties;
import com.article.metaphor_llm_processor.orchestrator.exception.ProcessorException;
import com.article.metaphor_llm_processor.orchestrator.producer.ChunkProcessingMessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class MetaphorReprocessingOrchestrator extends ProcessingOrchestrator {

    private static final Map<ProcessingMilestone, DocumentChunkStatus> REPROCESSING_MILESTONE_STATUS_TRANSITION_MAP = Map.of(
            ProcessingMilestone.LEXICAL_UNIT_PROCESSING, DocumentChunkStatus.LEXICAL_UNIT_PROCESSING__PENDING,
            ProcessingMilestone.DICTIONARY_ACCESS, DocumentChunkStatus.DICTIONARY_ACCESS__PENDING,
            ProcessingMilestone.METAPHOR_ANALYSIS, DocumentChunkStatus.METAPHOR_ANALYSIS__PENDING
    );

    private static final Map<DocumentChunkStatus, String> PREVIOUS_STATUS_MAPPING = Map.of(
            DocumentChunkStatus.DICTIONARY_ACCESS__PENDING, ProcessingMilestone.LEXICAL_UNIT_PROCESSING.name(),
            DocumentChunkStatus.METAPHOR_ANALYSIS__PENDING, ProcessingMilestone.DICTIONARY_ACCESS.name()
    );


    private final IndexedDocumentRepository documentRepository;
    private final IndexedDocumentChunkRepository chunkRepository;
    private final DocumentProcessingStateManagerClient client;

    public MetaphorReprocessingOrchestrator(IndexedDocumentRepository documentRepository,
                                            IndexedDocumentChunkRepository chunkRepository,
                                            ChunkProcessingMessageProducer chunkProcessingMessageProducer,
                                            ProcessingConfigProperties processingConfigProperties,
                                            DocumentProcessingStateManagerClient client) {
        super(documentRepository, chunkRepository, chunkProcessingMessageProducer, processingConfigProperties);
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.client = client;
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

    @Override
    ChunkProcessingData createChunkProcessingData(IndexedDocumentChunk chunk) {
        log.info("Creating a chunk processing data input for reprocessing from chunk {}", chunk.getId());
        return client.getChunkProcessingData(chunk.getId(), getPreviousStateBeforeReprocessing(chunk));
    }

    private String getPreviousStateBeforeReprocessing(IndexedDocumentChunk chunk) {
        DocumentChunkStatus status = chunk.getStatus();
        String previousState = PREVIOUS_STATUS_MAPPING.get(status);
        if (previousState == null) {
            throw new ProcessorException(String.format("Document[id = %s] has invalid status for reprocessing: %s",
                    chunk.getId(), status)
            );
        }

        return previousState;
    }

    private void reprocessChunkByRequestedDocument(IndexedDocument document) {
        log.info("Document[id = {}, status = {}] is chosen. Its next chunk is about to be processed.",
                document.getId(), document.getStatus()
        );

        Optional<IndexedDocumentChunk> chunkOptional = chunkRepository.findFirstChunkEligibleForProcessingByDocumentId(
                document.getId()
        );
        if (chunkOptional.isEmpty()) {
            log.info("There is no chunk associated with document {}", document.getId());
            document.setStatus(DocumentStatus.DONE);
            documentRepository.save(document);
            return;
        }

        if (document.getStatus() != DocumentStatus.REPROCESSING) {
            document.setStatus(DocumentStatus.REPROCESSING);
            documentRepository.save(document);
        }

        reprocessChunk(chunkOptional.get());
    }

    private void reprocessChunkAfterFailure(IndexedDocumentChunk chunk) {
        DocumentChunkStatus currentStatus = chunk.getStatus();
        ProcessingMilestone currentMilestone = chunk.getMilestone();
        log.info("Chunk to be reprocessed[id = {}, current status = {}, current milestone = {}]",
                chunk.getId(), currentStatus, currentMilestone
        );

        Optional<IndexedDocument> documentOptional = documentRepository.findById(chunk.getDocumentId());
        if (documentOptional.isEmpty()) {
            failChunkAndSucceedingChunks(chunk, "Chunk document not found");
        } else {
            reprocessChunk(chunk);
        }
    }

    private void reprocessChunk(IndexedDocumentChunk chunk) {
        try {
            ProcessingMilestone milestone = chunk.getMilestone();
            log.info("Reprocess chunk: {} with milestone {}", chunk.getId(), milestone);
            DocumentChunkStatus newStatus = REPROCESSING_MILESTONE_STATUS_TRANSITION_MAP.getOrDefault(
                    milestone, DocumentChunkStatus.LEXICAL_UNIT_PROCESSING__PENDING
            );
            doProcess(chunk, newStatus);
        } catch (Exception e) {
            log.error("Reprocessing failed due to {}", e.getMessage(), e);
            failChunkAndSucceedingChunks(chunk, e.getMessage());
        }
    }
}
