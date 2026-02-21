package com.article.metaphor.llm.processor.article_indexer.repository;

import com.article.metaphor.llm.processor.common.model.DocumentIndexingFailure;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DocumentIndexingFailureRepository extends MongoRepository<DocumentIndexingFailure, String> {

    @Aggregation(pipeline = {
            "{$match: { 'status': 'ELIGIBLE_FOR_RETRY'}}",
            "{$sort: {'lastIndexingAttempt': 1}}",
            "{$limit: 1}"
    })
    Optional<DocumentIndexingFailure> findOldestAttemptedFailureEligibleForRetry();
}
