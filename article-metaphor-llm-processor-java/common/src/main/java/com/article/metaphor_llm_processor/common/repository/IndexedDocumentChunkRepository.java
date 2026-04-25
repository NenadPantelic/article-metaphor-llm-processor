package com.article.metaphor_llm_processor.common.repository;

import com.article.metaphor_llm_processor.common.model.IndexedDocumentChunk;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.util.List;
import java.util.Optional;

public interface IndexedDocumentChunkRepository extends MongoRepository<IndexedDocumentChunk, String> {

    @Aggregation(pipeline = {
            "{$match: { 'documentId': ?0, 'status': {$in: ['PENDING', 'NEXT_ATTEMPT_NEEDED', 'PENDING_REPROCESSING']}}}",
            "{$sort: {'order': 1}}",
            "{$limit: 1}"
    })
    Optional<IndexedDocumentChunk> findFirstChunkEligibleForProcessingByDocumentId(String documentId);


    @Aggregation(pipeline = {
            "{$match: { 'status': 'PENDING'}}",
            "{$sort: {'order': 1}}",
            "{$limit: 1}"
    })
    Optional<IndexedDocumentChunk> findFirstChunkEligibleForProcessing();

    @Aggregation(pipeline = {
            "{$match: { 'status': {$in: ['ANOTHER_ATTEMPT_NEEDED', 'PENDING_REPROCESSING']}}}",
            "{$sort: {'order': 1}}",
            "{$limit: 1}"
    })
    Optional<IndexedDocumentChunk> findFirstChunkEligibleForReprocessing();


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


    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'status': ?1 } }")
    long updateChunkState(String id, String chunkStatus);

    @Query("{ '_documentId': ?0, 'order': {$gte: ?1 }}")
    List<IndexedDocumentChunk> findByDocumentIdAndOrderGreaterThanOrEq(String documentId, int order);

    @Aggregation(pipeline = {
            """
                    {$match: {
                     'status': {
                           $in: ['STARTED_PROCESSING', 'LEXICAL_UNIT_PROCESSING__PENDING',
                           'LEXICAL_UNIT_PROCESSING__IN_PROGRESS', 'DICTIONARY_ACCESS__PENDING',
                           'DICTIONARY_ACCESS__IN_PROGRESS', 'METAPHOR_ANALYSIS__PENDING', 'METAPHOR_ANALYSIS__IN_PROGRESS'
                           ]},
                           'lastProcessingAttemptedAt': {$gte: ?1}}
                           """,
            "{$limit: ?2}"
    })
        // Example:
        // now: 19:00
        // time limit: 6h
        // 13:00 threshold

        // executed: 11:30 - YES
        // executed: 14:00 - NO
    List<IndexedDocumentChunk> findStuckChunksInProcessing(int tooLongExecutionTimeThreshold, int limit);
}

