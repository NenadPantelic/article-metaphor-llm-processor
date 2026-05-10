package com.article.metaphor_llm_processor.chunk_processing.state_manager.repository;

import com.article.metaphor_llm_processor.chunk_processing.state_manager.model.ChunkProcessingState;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChunkProcessingStateRepository extends MongoRepository<ChunkProcessingState, String> {

    Optional<ChunkProcessingState> findByChunkId(String chunkId);
}
