package com.article.metaphor_llm_processor.orchestrator.finalizer;

import com.article.metaphor_llm_processor.common.model.DocumentChunkState;
import com.article.metaphor_llm_processor.common.model.DocumentState;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentRepository;
import com.article.metaphor_llm_processor.orchestrator.dto.ProcessedChunk;
import com.article.metaphor_llm_processor.orchestrator.model.ChunkProcessingState;
import com.article.metaphor_llm_processor.orchestrator.repository.ChunkProcessingStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class ChunkProcessingFinalizer {

    private final IndexedDocumentRepository documentRepository;
    private final IndexedDocumentChunkRepository chunkRepository;
    private final ChunkProcessingStateRepository chunkProcessingStateRepository;

    public ChunkProcessingFinalizer(IndexedDocumentRepository documentRepository,
                                    IndexedDocumentChunkRepository chunkRepository,
                                    ChunkProcessingStateRepository chunkProcessingStateRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.chunkProcessingStateRepository = chunkProcessingStateRepository;
    }

    @RabbitListener(queues = "q.processed-chunks")
    public void execute(ProcessedChunk processedChunk) {
        String chunkId = processedChunk.chunkId();
        log.info("Received processed chunk: {}", processedChunk.chunkId());

        Optional<ChunkProcessingState> chunkProcessingStateOpt = chunkProcessingStateRepository.findByChunkId(chunkId);
        if (chunkProcessingStateOpt.isEmpty()) {
            log.error("Chunk processing state for chunk with id {} does not exist, skipping any check or update", chunkId);
            return;
        }

        ChunkProcessingState chunkProcessingState = chunkProcessingStateOpt.get();
        String documentId = chunkProcessingState.getDocumentId();

        long noOfProcessedChunks = chunkRepository.updateChunkState(chunkId, DocumentChunkState.COMPLETED.name());
        if (noOfProcessedChunks == 0) {
            log.error("Chunk[id = {}] has not been marked as completed.", chunkId);
            return;
        }

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

        // TODO: invoke assembler
    }

}
