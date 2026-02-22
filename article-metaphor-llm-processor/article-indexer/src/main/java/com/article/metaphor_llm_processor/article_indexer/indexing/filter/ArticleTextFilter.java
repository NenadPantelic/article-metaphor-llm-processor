package com.article.metaphor.llm.processor.article_indexer.indexing.filter;

import org.springframework.ai.document.Document;

import java.util.List;

public interface ArticleTextFilter {

    List<Document> filterDocuments(List<Document> documents);
}
