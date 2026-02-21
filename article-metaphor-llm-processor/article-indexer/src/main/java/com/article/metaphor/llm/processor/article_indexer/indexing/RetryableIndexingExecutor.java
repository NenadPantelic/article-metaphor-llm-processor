package com.article.metaphor.llm.processor.article_indexer.indexing;

import com.article.metaphor.llm.processor.article_indexer.configproperties.IndexingConfigProperties;
import com.article.metaphor.llm.processor.common.model.IndexedDocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RetryableIndexingExecutor {

    private final DocumentIndexingService documentIndexingService;
    private final int maxRetryAttempts;


    public RetryableIndexingExecutor(DocumentIndexingService documentIndexingService,
                                     IndexingConfigProperties indexingConfigProperties) {
        this.documentIndexingService = documentIndexingService;
        this.maxRetryAttempts = indexingConfigProperties.maxRetry();
    }


    public IndexingReport tryInitialIndexing(String source, String origin) {
        // initial indexing - 0 attemptsSoFar
        return tryIndexing(source, origin, 0);
    }

    public IndexingReport tryIndexing(String source, String origin, int attemptsSoFar) {
        int attemptNo = attemptsSoFar + 1;
        try {
            List<IndexedDocumentChunk> chunks = documentIndexingService.indexFromURL(source, origin);
            log.info("Document indexing [path = {}, origin = {}] has passed with {} attempt", source, origin, attemptNo);
            return new IndexingReport(chunks, null, false);
        } catch (Exception e) {
            // TODO: retry should not be allowed for all exception types
            boolean retryPossible = attemptNo < maxRetryAttempts;
            log.error("An indexing of the document from {} failed. Retry possible: {}", source, retryPossible, e);
            return new IndexingReport(null, e, retryPossible);
        }
    }
}
