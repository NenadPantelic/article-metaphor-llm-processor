package com.article.metaphor_llm_processor.orchestrator.repository;

import com.article.metaphor_llm_processor.orchestrator.model.DocumentReprocessingRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DocumentReprocessingRequestRepository extends MongoRepository<DocumentReprocessingRequest, String> {

    Optional<DocumentReprocessingRequest> findByDocumentId(String documentId);

    void deleteByDocumentId(String documentId);
}
