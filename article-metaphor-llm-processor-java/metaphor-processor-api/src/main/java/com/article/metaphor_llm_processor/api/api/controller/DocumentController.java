package com.article.metaphor_llm_processor.api.api.controller;

import com.article.metaphor_llm_processor.api.dto.request.DocumentFilterRequest;
import com.article.metaphor_llm_processor.api.dto.response.Document;
import com.article.metaphor_llm_processor.api.dto.response.DocumentItem;
import com.article.metaphor_llm_processor.api.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    List<DocumentItem> filterDocuments(@RequestBody DocumentFilterRequest documentFilterRequest) {
        log.info("Received a request to filter documents");
        return documentService.filterDocuments(documentFilterRequest);
    }

    @GetMapping("/{documentId}")
    Document getDocument(@PathVariable("documentId") String documentId) {
        return documentService.getDocument(documentId);
    }
}
