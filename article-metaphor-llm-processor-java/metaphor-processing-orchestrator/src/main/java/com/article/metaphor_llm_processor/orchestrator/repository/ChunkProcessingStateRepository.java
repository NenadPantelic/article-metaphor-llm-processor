package com.article.metaphor_llm_processor.orchestrator.repository;

import com.article.metaphor_llm_processor.orchestrator.model.ChunkProcessingState;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChunkProcessingStateRepository extends MongoRepository<ChunkProcessingState, String> {

    Optional<ChunkProcessingState> findByChunkId(String chunkId);

    @Aggregation(pipeline = {"{$match: {'active': true, $expr: { $gte: [{ $size: 'errors' }, $1] }}", "{$limit: ?2}"})
    List<ChunkProcessingState> findActiveStatesWithTooManyFailures(int numOfFailures, int limit);
}
