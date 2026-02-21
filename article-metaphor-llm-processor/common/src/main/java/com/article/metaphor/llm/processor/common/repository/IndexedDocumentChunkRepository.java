package com.article.metaphor.llm.processor.common.repository;

import com.article.metaphor.llm.processor.common.model.IndexedDocumentChunk;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IndexedDocumentChunkRepository extends MongoRepository<IndexedDocumentChunk, String> {

    @Aggregation(pipeline = {
            "{$match: { 'documentId': ?0, 'status': {$in: ['PENDING', 'NEXT_ATTEMPT_NEEDED', 'PENDING_REPROCESSING']}}}",
            "{$sort: {'order': 1}}",
            "{$limit: 1}"
    })
    Optional<IndexedDocumentChunk> findFirstChunkEligibleForProcessing(String documentId);

    List<IndexedDocumentChunk> findByDocumentId(String documentId);

    int countByDocumentId(String documentId);

    @Query(value = "{'status': 'SUCCESSFULLY_PROCESSED'}", count = true)
    int countSuccessfullyProcessedByDocumentId(String documentId);

    @Query(value = "{'status': 'FAILED_TO_PROCESS'}", count = true)
    int countProcessingFailuresByDocumentId(String documentId);

    @Aggregation(pipeline = {
            "{$match: { 'documentId': ?0, 'order': {$lt: ?1}}}",
            "{$project: { 'length': { '$strLenCP': '$text'}}}",
            "{$group: {'_id': null, 'totalLength': {'$sum': '$length'}}}",
            "{$project: { 'totalLength': 1, '_id': 0}}"
    })
    Integer findCumulativeLengthOfPreviousChunks(String documentId, int order);
}
