package com.article.metaphor_llm_processor.orchestrator.finalizer;

import com.article.metaphor_llm_processor.common.model.DocumentChunkState;
import com.article.metaphor_llm_processor.common.model.DocumentState;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentRepository;
import com.article.metaphor_llm_processor.orchestrator.configproperties.ProcessingConfigProperties;
import com.article.metaphor_llm_processor.orchestrator.dto.ProcessedChunk;
import com.article.metaphor_llm_processor.orchestrator.model.ChunkProcessingError;
import com.article.metaphor_llm_processor.orchestrator.model.ChunkProcessingState;
import com.article.metaphor_llm_processor.orchestrator.repository.ChunkProcessingStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class ChunkProcessingFinalizer {

    private final IndexedDocumentRepository documentRepository;
    private final IndexedDocumentChunkRepository chunkRepository;
    private final ChunkProcessingStateRepository chunkProcessingStateRepository;
    private final AnalysisResultAssembler analysisResultAssembler;
    private final int maxNoOfAttempts;

    public ChunkProcessingFinalizer(IndexedDocumentRepository documentRepository,
                                    IndexedDocumentChunkRepository chunkRepository,
                                    ChunkProcessingStateRepository chunkProcessingStateRepository,
                                    AnalysisResultAssembler analysisResultAssembler,
                                    ProcessingConfigProperties processingConfigProperties) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.chunkProcessingStateRepository = chunkProcessingStateRepository;
        this.analysisResultAssembler = analysisResultAssembler;
        this.maxNoOfAttempts = processingConfigProperties.maxAttemptNo();
    }

    @RabbitListener(queues = "q.processed-chunks")
    @Transactional
    public void execute(ProcessedChunk processedChunk) {
        String chunkId = processedChunk.chunkId();
        log.info("Received processed chunk: {}", chunkId);

        Optional<ChunkProcessingState> chunkProcessingStateOpt = chunkProcessingStateRepository.findByChunkId(chunkId);
        if (chunkProcessingStateOpt.isEmpty()) {
            log.error("Chunk processing state for chunk with id {} does not exist, skipping any check or update", chunkId);
            return;
        }
        ChunkProcessingState chunkProcessingState = chunkProcessingStateOpt.get();
        ChunkProcessingError error = processedChunk.error();
        chunkProcessingState.addError(error);
        chunkProcessingState.stopProcessing();
        chunkProcessingStateRepository.save(chunkProcessingState);

        DocumentChunkState newChunkState;
        if (error == null) {
            newChunkState = DocumentChunkState.COMPLETED;
        } else {
            if (chunkProcessingState.getErrorCount() < maxNoOfAttempts && error.reprocessable()) {
                newChunkState = DocumentChunkState.REPROCESSING_NEEDED;
            } else {
                newChunkState = DocumentChunkState.FAILED;
            }
        }

        long noOfProcessedChunks = chunkRepository.updateChunkState(chunkId, newChunkState.name());
        if (noOfProcessedChunks == 0) {
            log.error("Chunk[id = {}] has not been marked as {}.", chunkId, newChunkState);
            return;
        } else {
            log.info("Chunk[id = {}] has been marked as {}.", chunkId, newChunkState);
        }

        String documentId = chunkProcessingState.getDocumentId();
        if (newChunkState == DocumentChunkState.COMPLETED || newChunkState == DocumentChunkState.FAILED) {
            markDocumentAsCompletedIfLastChunk(documentId);
        }

        if (newChunkState == DocumentChunkState.COMPLETED) {
            analysisResultAssembler.assembleAnalysisResults(documentId);
        }
    }

    private void markDocumentAsCompletedIfLastChunk(String documentId) {
        log.info("Running a completeness check for document: {}", documentId);
        int totalNumOfChunks = chunkRepository.countByDocumentId(documentId);
        int numOfProcessedChunks = chunkRepository.countSuccessfullyProcessedByDocumentId(documentId);
        if (totalNumOfChunks != numOfProcessedChunks) {
            log.info("Number of processed chunk does not match the total number of chunks for document '{}', " +
                    "there are more which processing has not been completed yet", documentId);
            return;
        }

        long numOfUpdatedDocs = documentRepository.updateDocumentState(documentId, DocumentState.PROCESSED_SUCCESSFULLY.name());
        if (numOfUpdatedDocs == 0) {
            log.error("Document[id = {}] has not been marked as completed.", documentId);
        }
    }
}
