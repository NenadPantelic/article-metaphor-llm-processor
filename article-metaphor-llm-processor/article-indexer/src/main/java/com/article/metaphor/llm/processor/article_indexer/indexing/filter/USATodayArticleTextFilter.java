package com.article.metaphor.llm.processor.article_indexer.indexing.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class USATodayArticleTextFilter implements ArticleTextFilter {

    private static final String TEXT_START_MARKER = "Add Topic";
    private static final String TEXT_END_MARKER = "Featured Weekly Ad";

    @Override
    public List<Document> filterDocuments(List<Document> documents) {
        return documents.stream().map(document -> {
            String formattedContent = document.getFormattedContent();

            int startIndex = formattedContent.indexOf(TEXT_START_MARKER);
            if (startIndex == -1) {
                log.info("Start marker {} not found. The whole document will be used.", TEXT_START_MARKER);
                return document;
            }

            int endIndex = formattedContent.indexOf(TEXT_END_MARKER);
            if (endIndex == -1) {
                log.info("End marker {} not found. The whole document will be used.", TEXT_END_MARKER);
                return document;
            }

            String content = formattedContent.substring(startIndex + TEXT_START_MARKER.length(), endIndex).strip();
            return new Document(content);
        }).collect(Collectors.toList());
    }
}