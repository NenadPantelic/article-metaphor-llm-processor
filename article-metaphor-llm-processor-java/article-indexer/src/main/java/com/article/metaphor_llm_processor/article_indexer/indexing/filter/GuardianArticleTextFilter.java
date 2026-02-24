package com.article.metaphor_llm_processor.article_indexer.indexing.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GuardianArticleTextFilter implements ArticleTextFilter {

    private static final String START_MARKER = "View image in fullscreen";
    private static final String END_MARKER = "Explore more on these topics";
    private static final String LINE_TO_SKIP_MARKER = "Photograph";
    private static final List<String> LINES_TO_SKIP = List.of("Share");

    @Override
    public List<Document> filterDocuments(List<Document> documents) {
        return documents.stream().map(document -> {
            try {
                String formattedContent = document.getFormattedContent();

                int startIndex = formattedContent.indexOf(START_MARKER);
                if (startIndex == -1) {
                    return document;
                }

                int endIndex = formattedContent.indexOf(END_MARKER);
                if (endIndex == -1) {
                    return document;
                }

                String[] lines = formattedContent.substring(startIndex + START_MARKER.length(), endIndex).split("\n");
                List<String> linesToTake = new ArrayList<>();

                for (String line : lines) {
                    if (line.contains(LINE_TO_SKIP_MARKER) || LINES_TO_SKIP.contains(line.strip())) {
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
