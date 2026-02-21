package com.article.metaphor.llm.processor.article_indexer.indexing;

import com.article.metaphor.llm.processor.article_indexer.indexing.filter.*;
import com.metaphor.llm.processor.article_indexer.indexing.filter.*;
import com.article.metaphor.llm.processor.common.model.Origin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HTMLArticleDocumentReader {

    private final Map<String, ArticleTextFilter> articleTextFilerMap;

    public HTMLArticleDocumentReader(GuardianArticleTextFilter guardianArticleTextFilter,
                                     IndependentArticleTextFilter independentArticleTextFilter,
                                     DailyMailArticleTextFilter dailyMailArticleTextFilter,
                                     USATodayArticleTextFilter usaTodayArticleTextFilter) {
        articleTextFilerMap = Map.of(
                Origin.GUARDIAN.name().toLowerCase(), guardianArticleTextFilter,
                Origin.INDEPENDENT.name().toLowerCase(), independentArticleTextFilter,
                Origin.DAILY_MAIL.name().toLowerCase(), dailyMailArticleTextFilter,
                Origin.USA_TODAY.name().toLowerCase(), usaTodayArticleTextFilter
        );
    }

    public List<Document> readFrom(String origin, Resource resource) {
        log.info("Reading a document from {}", resource.getDescription());
        List<Document> documents = new TikaDocumentReader(resource).read();
        ArticleTextFilter articleTextFilter = articleTextFilerMap.get(origin.toLowerCase());
        if (articleTextFilter == null) {
            log.warn("For {} there is no registered article text filter. The document will not be filtered, " +
                    "but returned as is instead", origin);
            return documents;
        }

        return articleTextFilter.filterDocuments(documents);
    }
}
