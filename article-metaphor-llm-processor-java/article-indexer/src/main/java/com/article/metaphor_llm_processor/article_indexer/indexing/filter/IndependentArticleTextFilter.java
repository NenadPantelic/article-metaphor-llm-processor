package com.article.metaphor_llm_processor.article_indexer.indexing.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class IndependentArticleTextFilter implements ArticleTextFilter {

    private static final String HEADER_START_MARKER = "next article";
    private static final String HEADER_END_MARKER = "Comments";
    private static final String BODY_START_MARKER = "Read our Privacy notice";
    private static final String BODY_END_MARKER = "Join our commenting forum";

    @Override
    public List<Document> filterDocuments(List<Document> documents) {
        return documents.stream().map(document -> {
            String formattedContent = document.getFormattedContent();

            int headerStartIndex = formattedContent.indexOf(HEADER_START_MARKER);
            if (headerStartIndex == -1) {
                log.info("Header start marker {} not found. The whole document will be used.", HEADER_START_MARKER);
                return document;
            }

            int headerEndIndex = formattedContent.indexOf(HEADER_END_MARKER);
            if (headerEndIndex == -1) {
                log.info("Header end marker {} not found. The whole document will be used.", HEADER_END_MARKER);
                return document;
            }

            String header = formattedContent.substring(headerStartIndex + HEADER_START_MARKER.length(), headerEndIndex);

            int bodyStartIndex = formattedContent.indexOf(BODY_START_MARKER);
            if (bodyStartIndex == -1) {
                log.info("Body start marker {} not found. The whole document will be used.", BODY_START_MARKER);
                return document;
            }

            int bodyEndIndex = formattedContent.indexOf(BODY_END_MARKER);
            if (bodyEndIndex == -1) {
                log.info("Body end marker {} not found. The whole document will be used.", BODY_END_MARKER);
                return document;
            }

            String body = formattedContent.substring(bodyStartIndex + BODY_START_MARKER.length(), bodyEndIndex);
            return new Document(header + body); // clear content
        }).collect(Collectors.toList());
    }
}