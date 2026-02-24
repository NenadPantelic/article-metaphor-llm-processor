package com.article.metaphor_llm_processor.article_indexer.indexing;

import com.article.metaphor_llm_processor.common.model.DocumentChunkStatus;
import com.article.metaphor_llm_processor.common.model.IndexedDocumentChunk;
import com.article.metaphor_llm_processor.common.model.OriginType;
import com.article.metaphor_llm_processor.article_indexer.model.DocumentIndexingAttempt;
import com.article.metaphor_llm_processor.article_indexer.model.DocumentIndexingFailure;
import com.article.metaphor_llm_processor.article_indexer.model.DocumentIndexingFailureStatus;
import com.article.metaphor_llm_processor.article_indexer.repository.DocumentIndexingFailureRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class DocumentIndexingFailureRetryProcessorTest {

    private final DocumentIndexingFailureRepository indexingFailureRepository = Mockito.mock(DocumentIndexingFailureRepository.class);
    private final RetryableIndexingExecutor retryableIndexingExecutor = Mockito.mock(RetryableIndexingExecutor.class);

    private final DocumentIndexingFailureRetryProcessor documentIndexingFailureRetryProcessor = new DocumentIndexingFailureRetryProcessor(
            indexingFailureRepository, retryableIndexingExecutor
    );


    @Test
    public void testProcessWhenThereIsNoFailureToRetry() {
        Mockito.doReturn(Optional.empty())
                .when(indexingFailureRepository)
                .findOldestAttemptedFailureEligibleForRetry();

        documentIndexingFailureRetryProcessor.process();
        Mockito.verify(indexingFailureRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void testProcessWhenRetriedIndexingPassed() {
        String id = "test-id";
        String source = "test-source";
        String origin = "test-origin";
        var now = Instant.now();
        var attempt = new DocumentIndexingAttempt(
                "test-error", now
        );
        var attempts = List.of(attempt);
        var documentIndexingFailure = new DocumentIndexingFailure(
                id, source, origin, OriginType.URL, now, attempts,
                DocumentIndexingFailureStatus.ELIGIBLE_FOR_RETRY, now, now
        );

        Mockito.doReturn(Optional.of(documentIndexingFailure))
                .when(indexingFailureRepository)
                .findOldestAttemptedFailureEligibleForRetry();

        var chunk = new IndexedDocumentChunk(
                "test-chunk-id", "test-document-id", "text", DocumentChunkStatus.PENDING, 1,
                null, null, null, null
        );
        var indexingReport = new IndexingReport(List.of(chunk), null, false);
        Mockito.doReturn(indexingReport).when(retryableIndexingExecutor).tryIndexing(source, origin, 1);

        documentIndexingFailureRetryProcessor.process();
        Mockito.verify(
                indexingFailureRepository, Mockito.times(1)
        ).deleteById(ArgumentMatchers.eq(id));
    }

    @Test
    public void testProcessWhenRetryableFailureHappened() {
        String id = "test-id";
        String source = "test-source";
        String origin = "test-origin";
        var now = Instant.now();
        var attempt = new DocumentIndexingAttempt(
                "test-error", now
        );
        List<DocumentIndexingAttempt> attempts = new ArrayList<>();
        attempts.add(attempt);
        var documentIndexingFailure = new DocumentIndexingFailure(
                id, source, origin, OriginType.URL, now, attempts,
                DocumentIndexingFailureStatus.ELIGIBLE_FOR_RETRY, now, now
        );
        Mockito.doReturn(Optional.of(documentIndexingFailure))
                .when(indexingFailureRepository)
                .findOldestAttemptedFailureEligibleForRetry();

        Exception exception = new RuntimeException("Document indexing failed.");
        var indexingReport = new IndexingReport(null, exception, true);
        Mockito.doReturn(indexingReport).when(retryableIndexingExecutor).tryIndexing(source, origin, 1);

        ArgumentCaptor<DocumentIndexingFailure> argumentCaptor = ArgumentCaptor.forClass(
                DocumentIndexingFailure.class
        );
        documentIndexingFailureRetryProcessor.process();

        Mockito.verify(
                indexingFailureRepository, Mockito.times(1)
        ).save(argumentCaptor.capture());

        var actualDocumentIndexingFailure = argumentCaptor.getValue();

        Assertions.assertThat(actualDocumentIndexingFailure.getId()).isEqualTo(id);
        Assertions.assertThat(actualDocumentIndexingFailure.getSource()).isEqualTo(source);
        Assertions.assertThat(actualDocumentIndexingFailure.getOrigin()).isEqualTo(origin);

        var failureAttempts = actualDocumentIndexingFailure.getAttempts();

        Assertions.assertThat(failureAttempts.size()).isEqualTo(2);
        Assertions.assertThat(failureAttempts.get(0)).isEqualTo(attempt);
        // TODO: add a time provider to assert timestamp values too
        Assertions.assertThat(failureAttempts.get(1).error()).isEqualTo(exception.getMessage());
        Assertions.assertThat(actualDocumentIndexingFailure.getStatus()).isEqualTo(DocumentIndexingFailureStatus.ELIGIBLE_FOR_RETRY);
    }

    @Test
    public void testProcessWhenNonRetryableFailureHappened() {
        String id = "test-id";
        String source = "test-source";
        String origin = "test-origin";
        var now = Instant.now();
        var attempt = new DocumentIndexingAttempt(
                "test-error", now
        );
        List<DocumentIndexingAttempt> attempts = new ArrayList<>();
        attempts.add(attempt);

        var documentIndexingFailure = new DocumentIndexingFailure(
                id, source, origin, OriginType.URL, now, attempts,
                DocumentIndexingFailureStatus.ELIGIBLE_FOR_RETRY, now, now
        );
        Mockito.doReturn(Optional.of(documentIndexingFailure))
                .when(indexingFailureRepository)
                .findOldestAttemptedFailureEligibleForRetry();

        Exception exception = new RuntimeException("Document indexing failed.");
        var indexingReport = new IndexingReport(null, exception, false);
        Mockito.doReturn(indexingReport).when(retryableIndexingExecutor).tryIndexing(source, origin, 1);

        ArgumentCaptor<DocumentIndexingFailure> argumentCaptor = ArgumentCaptor.forClass(
                DocumentIndexingFailure.class
        );
        documentIndexingFailureRetryProcessor.process();

        Mockito.verify(
                indexingFailureRepository, Mockito.times(1)
        ).save(argumentCaptor.capture());

        var actualDocumentIndexingFailure = argumentCaptor.getValue();

        Assertions.assertThat(actualDocumentIndexingFailure.getId()).isEqualTo(id);
        Assertions.assertThat(actualDocumentIndexingFailure.getSource()).isEqualTo(source);
        Assertions.assertThat(actualDocumentIndexingFailure.getOrigin()).isEqualTo(origin);

        var failureAttempts = actualDocumentIndexingFailure.getAttempts();

        Assertions.assertThat(failureAttempts.size()).isEqualTo(2);
        Assertions.assertThat(failureAttempts.get(0)).isEqualTo(attempt);
        // TODO: add a time provider to assert timestamp values too
        Assertions.assertThat(failureAttempts.get(1).error()).isEqualTo(exception.getMessage());
        Assertions.assertThat(actualDocumentIndexingFailure.getStatus()).isEqualTo(DocumentIndexingFailureStatus.ALL_ATTEMPTS_FAILED);
    }
}