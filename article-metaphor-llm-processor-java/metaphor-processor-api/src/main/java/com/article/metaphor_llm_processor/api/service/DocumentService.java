package com.article.metaphor_llm_processor.api.service;

import com.article.metaphor_llm_processor.api.dto.request.DocumentFilterRequest;
import com.article.metaphor_llm_processor.api.dto.response.Document;
import com.article.metaphor_llm_processor.api.dto.response.DocumentItem;

import java.util.List;

public interface DocumentService {

    List<DocumentItem> filterDocuments(DocumentFilterRequest documentFilterRequest);

    Document getDocument(String documentId);
}
