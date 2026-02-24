package com.article.metaphor_llm_processor.article_indexer.indexing;

import ai.metaphor.metaphor_llm_processor.model.DocumentChunkStatus;
import ai.metaphor.metaphor_llm_processor.model.IndexedDocumentChunk;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class IndexingReportTest {

    @Test
    public void testPassedWhenChunksIsNull() {
        var indexingReport = new IndexingReport(null, null, true);
        Assertions.assertThat(indexingReport.passed()).isFalse();
    }

    @Test
    public void testPassedWhenExceptionIsNotNull() {
        var now = Instant.now();
        var chunk = new IndexedDocumentChunk(
                "test-id-1", "test-document-id-1", "Test 1234567890", DocumentChunkStatus.PENDING, 1,
                null, now, now, now
        );

        var indexingReport = new IndexingReport(
                List.of(chunk), new RuntimeException("Exception occurred"), true
        );
        Assertions.assertThat(indexingReport.passed()).isFalse();
    }

    @Test
    public void testRetryableExceptionOccurredWhenExceptionIsNull() {
        var now = Instant.now();
        var chunk = new IndexedDocumentChunk(
                "test-id-1", "test-document-id-1", "Test 1234567890", DocumentChunkStatus.PENDING, 1,
                null, now, now, now
        );

        var indexingReport = new IndexingReport(List.of(chunk), null, true);
        Assertions.assertThat(indexingReport.hasException()).isFalse();
        Assertions.assertThat(indexingReport.retryableExceptionOccurred()).isFalse();
    }

    @Test
    public void testRetryableExceptionOccurredWhenNotRetryable() {
        var now = Instant.now();
        var chunk = new IndexedDocumentChunk(
                "test-id-1", "test-document-id-1", "Test 1234567890", DocumentChunkStatus.PENDING, 1,
                null, now, now, now
        );

        var indexingReport = new IndexingReport(
                List.of(chunk), new RuntimeException("Exception occurred"), false
        );
        Assertions.assertThat(indexingReport.hasException()).isTrue();
        Assertions.assertThat(indexingReport.retryableExceptionOccurred()).isFalse();
    }

    @Test
    public void testRetryableExceptionOccurredWhenRetryable() {
        var now = Instant.now();
        var chunk = new IndexedDocumentChunk(
                "test-id-1", "test-document-id-1", "Test 1234567890", DocumentChunkStatus.PENDING, 1,
                null, now, now, now
        );

        var indexingReport = new IndexingReport(
                List.of(chunk), new RuntimeException("Exception occurred"), true
        );
        Assertions.assertThat(indexingReport.hasException()).isTrue();
        Assertions.assertThat(indexingReport.retryableExceptionOccurred()).isTrue();
    }
}