package com.article.metaphor.llm.processor.article_indexer.indexing.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DailyMailArticleTextFilter implements ArticleTextFilter {

    private static final String START_MARKER = "Advertisement";
    private static final String END_MARKER = "Share or comment on this article";

    @Override
    public List<Document> filterDocuments(List<Document> documents) {
        return documents.stream().map(document -> {
            try {
                String formattedContent = document.getFormattedContent();
                // when fetched, its content is heavily stretched out with each text line separated by multiple empty
                // lines; this is to remove unnecessary empty lines
                int startIndex = formattedContent.indexOf(START_MARKER);
                if (startIndex == -1) {
                    return document;
                }

                int endIndex = formattedContent.indexOf(END_MARKER);
                if (endIndex == -1) {
                    return document;
                }

                formattedContent = formattedContent.substring(startIndex + START_MARKER.length(), endIndex);
                String[] lines = formattedContent.split("\n");
                List<String> linesToTake = new ArrayList<>();

                for (String line : lines) {
                    line = line.strip();
                    if (line.isEmpty() || "\n".equals(line)) {
                        continue;
                    }

                    linesToTake.add(line);
                }

                String cleanContent = String.join("\n", linesToTake);
                return new Document(cleanContent);
            } catch (Exception e) {
                log.error("Unable to process the document[text = {}], returning the original one", document.getFormattedContent());
                return document;
            }
        }).collect(Collectors.toList());
    }
}
