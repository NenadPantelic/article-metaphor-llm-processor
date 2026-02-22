package com.article.metaphor.llm.processor.orchestrator.repository;

import com.article.metaphor.llm.processor.orchestrator.model.DocumentReprocessingRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DocumentReprocessingRequestRepository extends MongoRepository<DocumentReprocessingRequest, String> {

    Optional<DocumentReprocessingRequest> findByDocumentId(String documentId);

    void deleteByDocumentId(String documentId);
}
