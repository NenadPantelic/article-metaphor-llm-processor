package com.article.metaphor_llm_processor.common.repository;

import com.article.metaphor_llm_processor.common.model.IndexedDocument;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IndexedDocumentRepository extends MongoRepository<IndexedDocument, String> {

    @Aggregation(pipeline = {
            "{$match: { 'status': {$in: ['PENDING', 'PROCESSING', 'PENDING_REPROCESSING', 'REPROCESSING']}}}",
            "{$sort: {'createdAt': 1}}",
            "{$limit: 1}"
    })
    Optional<IndexedDocument> findOldestEligibleDocumentForProcessing();
}
