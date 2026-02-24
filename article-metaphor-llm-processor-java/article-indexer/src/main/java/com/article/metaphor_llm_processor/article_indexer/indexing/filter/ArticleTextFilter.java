package com.article.metaphor_llm_processor.article_indexer.indexing.filter;

import org.springframework.ai.document.Document;

import java.util.List;

public interface ArticleTextFilter {

    List<Document> filterDocuments(List<Document> documents);
}
