package com.article.metaphor_llm_processor.api.service.impl;

import com.article.metaphor_llm_processor.api.dto.request.DocumentFilterRequest;
import com.article.metaphor_llm_processor.api.dto.response.Document;
import com.article.metaphor_llm_processor.api.dto.response.DocumentItem;
import com.article.metaphor_llm_processor.api.mapper.DocumentMapper;
import com.article.metaphor_llm_processor.api.repository.DocumentRepository;
import com.article.metaphor_llm_processor.api.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_LIMIT = 50;

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;

    public DocumentServiceImpl(DocumentRepository documentRepository, DocumentMapper documentMapper) {
        this.documentRepository = documentRepository;
        this.documentMapper = documentMapper;
    }

    @Override
    public List<DocumentItem> filterDocuments(DocumentFilterRequest documentFilterRequest) {
        log.info("Filtering documents by {}", documentFilterRequest);
        List<org.bson.Document> documents = documentRepository.filter(
                documentFilterRequest.filter(),
                documentFilterRequest.sortBy(),
                documentFilterRequest.sortOrder(),
                documentFilterRequest.page() != null ? documentFilterRequest.page() : DEFAULT_PAGE,
                documentFilterRequest.limit() != null ? documentFilterRequest.limit() : DEFAULT_LIMIT
        );
        return documentMapper.bsonDocumentsToDocumentItems(documents);
    }

    @Override
    public Document getDocument(String documentId) {
        log.info("Get document by id {}", documentId);
        return documentMapper.bsonDocumentToDocument(documentRepository.findById(documentId));
    }
}
