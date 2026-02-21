package com.article.metaphor.llm.processor.article_indexer.indexing;

import com.article.metaphor.llm.processor.article_indexer.repository.DocumentIndexingFailureRepository;
import com.article.metaphor.llm.processor.common.model.DocumentIndexingAttempt;
import com.article.metaphor.llm.processor.common.model.DocumentIndexingFailure;
import com.article.metaphor.llm.processor.common.model.DocumentIndexingFailureStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class DocumentIndexingFailureRetryProcessor {

    private final DocumentIndexingFailureRepository indexingFailureRepository;
    private final RetryableIndexingExecutor retryableIndexingExecutor;

    public DocumentIndexingFailureRetryProcessor(DocumentIndexingFailureRepository indexingFailureRepository,
                                                 RetryableIndexingExecutor retryableIndexingExecutor) {
        this.indexingFailureRepository = indexingFailureRepository;
        this.retryableIndexingExecutor = retryableIndexingExecutor;
    }

    private void tryIndexingDocument(DocumentIndexingFailure documentIndexingFailure) {
        String source = documentIndexingFailure.getSource();
        String origin = documentIndexingFailure.getOrigin();

        var attemptsSoFar = documentIndexingFailure.getAttempts() == null ?
                0 : documentIndexingFailure.getAttempts().size();
        log.info("Try indexing an article[path = {}, origin = {}] which indexing previously failed {} time(s)",
                source, origin, attemptsSoFar + 1 // +1 for the initial one
        );

        Instant now = Instant.now();
        IndexingReport indexingReport = retryableIndexingExecutor.tryIndexing(source, origin, attemptsSoFar);

        if (indexingReport.passed()) {
            // all good, remove the failure record
            indexingFailureRepository.deleteById(documentIndexingFailure.getId());
        } else {
            var documentIndexingAttempt = new DocumentIndexingAttempt(indexingReport.getException().getMessage(), now);
            documentIndexingFailure.addIndexingAttempt(documentIndexingAttempt);
            documentIndexingFailure.setLastIndexingAttempt(now);

            if (!indexingReport.retryableExceptionOccurred()) {
                documentIndexingFailure.setStatus(DocumentIndexingFailureStatus.ALL_ATTEMPTS_FAILED);
            }

            indexingFailureRepository.save(documentIndexingFailure);
        }
    }

    /**
     * Process the document indexing failure.
     * It tries finding a failure eligible for retry and takes another shot.
     */
    @Scheduled(fixedDelayString = "#{@'indexing-com.metaphor.llm.processor.article_indexer.configproperties.IndexingConfigProperties'.retryIntervalInMillis}")
    public void process() {
        log.info("Try finding a document indexing failure eligible for retry...");
        var failureToProcessOptional = indexingFailureRepository.findOldestAttemptedFailureEligibleForRetry();

        if (failureToProcessOptional.isEmpty()) {
            log.info("No document indexing failure eligible for retry has been found");
            return;
        }

        var failureToProcess = failureToProcessOptional.get();
        tryIndexingDocument(failureToProcess);
    }
}
