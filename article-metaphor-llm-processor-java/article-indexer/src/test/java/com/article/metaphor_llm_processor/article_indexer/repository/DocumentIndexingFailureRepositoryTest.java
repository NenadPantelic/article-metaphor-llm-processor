package com.article.metaphor_llm_processor.article_indexer.repository;

import com.article.metaphor_llm_processor.article_indexer.model.DocumentIndexingFailure;
import com.article.metaphor_llm_processor.article_indexer.model.DocumentIndexingFailureStatus;
import com.article.metaphor_llm_processor.common.model.OriginType;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SpringBootTest
class DocumentIndexingFailureRepositoryTest {// extends DatabaseIntegrationSetup {

    @Autowired
    private DocumentIndexingFailureRepository documentIndexingFailureRepository;


    @Test
    public void whenCollectionIsEmptyFindOldestAttemptedFailureEligibleForRetryShouldReturnEmptyOptional() {
        cleanCollection();
        Optional<DocumentIndexingFailure> failureOptional = documentIndexingFailureRepository.findOldestAttemptedFailureEligibleForRetry();
        Assertions.assertThat(failureOptional.isEmpty()).isTrue();
    }

    @Test
    public void whenMultipleFailuresEligibleForRetryPresentShouldReturnTheOldestAttempted() {
        var failureRecords = populateDocumentIndexingFailureCollection(
                3, 3
        );

        Optional<DocumentIndexingFailure> failureOptional = documentIndexingFailureRepository.findOldestAttemptedFailureEligibleForRetry();

        Assertions.assertThat(failureOptional.isPresent()).isTrue();
        DocumentIndexingFailure documentIndexingFailure = failureOptional.get();

        // compare only ids because the timestamp precision format can differ and therefore the full object
        // comparison may fail
        Assertions.assertThat(documentIndexingFailure.getId()).isEqualTo(
                // the first one is the oldest attempted
                failureRecords.get(DocumentIndexingFailureStatus.ELIGIBLE_FOR_RETRY).get(0).getId()
        );
        cleanCollection();
    }

    @Test
    public void whenNoFailuresEligibleForRetryPresentShouldReturnAnEmptyOptional() {
        populateDocumentIndexingFailureCollection(
                0, 2
        );

        Optional<DocumentIndexingFailure> failureOptional = documentIndexingFailureRepository.findOldestAttemptedFailureEligibleForRetry();
        Assertions.assertThat(failureOptional.isEmpty()).isTrue();

        cleanCollection();
    }

    private Map<DocumentIndexingFailureStatus, List<DocumentIndexingFailure>> populateDocumentIndexingFailureCollection(
            int numOfEligibleForRetryRecords,
            int numOfAllAttemptsFailedRecords
    ) {
        var now = Instant.now();

        // ELIGIBLE_FOR_RETRY
        List<DocumentIndexingFailure> recordsEligibleForRetry = new ArrayList<>();
        for (int i = 0; i < numOfEligibleForRetryRecords; i++) {
            var failureEFR = DocumentIndexingFailure.builder()
                    .source(randomString())
                    .origin(randomString())
                    .type(OriginType.URL)
                    .status(DocumentIndexingFailureStatus.ELIGIBLE_FOR_RETRY)
                    .lastIndexingAttempt(now.plusMillis(i))
                    .build();
            recordsEligibleForRetry.add(failureEFR);
        }
        documentIndexingFailureRepository.saveAll(recordsEligibleForRetry);

        // ALL_ATTEMPTS_FAILED
        List<DocumentIndexingFailure> recordsAllAttemptsFailed = new ArrayList<>();
        for (int i = 0; i < numOfAllAttemptsFailedRecords; i++) {
            var failureAAF = DocumentIndexingFailure.builder()
                    .source(randomString())
                    .origin(randomString())
                    .type(OriginType.URL)
                    .status(DocumentIndexingFailureStatus.ALL_ATTEMPTS_FAILED)
                    .lastIndexingAttempt(now.plusMillis(i))
                    .build();
            recordsAllAttemptsFailed.add(failureAAF);
        }
        documentIndexingFailureRepository.saveAll(recordsAllAttemptsFailed);

        return Map.of(
                DocumentIndexingFailureStatus.ELIGIBLE_FOR_RETRY, recordsEligibleForRetry,
                DocumentIndexingFailureStatus.ALL_ATTEMPTS_FAILED, recordsAllAttemptsFailed
        );
    }

    private void cleanCollection() {
        documentIndexingFailureRepository.deleteAll();
    }

    private String randomString() {
        return RandomStringUtils.random(8, true, true);
    }
}