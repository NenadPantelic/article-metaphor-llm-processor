package com.article.metaphor_llm_processor.common.client;

import com.article.metaphor_llm_processor.common.dto.processing.ChunkProcessingData;
import com.article.metaphor_llm_processor.common.exception.DocumentProcessingStateManagerApiException;
import com.article.metaphor_llm_processor.common.exception.RetryableHttpClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class DocumentProcessingStateManagerClient {

    private static final Set<HttpStatusCode> RETRYABLE_STATE_CODES = Set.of(
            HttpStatusCode.valueOf(429),
            HttpStatusCode.valueOf(503),
            HttpStatusCode.valueOf(504)
    );

    private final RestClient client;

    public DocumentProcessingStateManagerClient(RestClient client) {
        this.client = client;
    }

    public ChunkProcessingData getChunkProcessingData(String chunkId, String state) {
        log.info("Get chunk processing data for: chunkId = {}, state = {}", chunkId, state);
        return execute(chunkId, state);
    }

    @Retryable(
            retryFor = {TimeoutException.class, IOException.class, RetryableHttpClientException.class},
            maxAttemptsExpression = "#{@documentProcessingStateManagerClientConfigProps.maxRetry()}",
            backoff = @Backoff(
                    delayExpression = "#{@documentProcessingStateManagerClientConfigProps.delay()}", // initial delay
                    multiplierExpression = "#{@documentProcessingStateManagerClientConfigProps.multiplier()}",
                    maxDelayExpression = "#{@documentProcessingStateManagerClientConfigProps.maxDelay()}" // max delay
            )
    )
    ChunkProcessingData execute(String chunkId, String state) {
        return client.get()
                .uri("/api/v1/processing-chunks/{chunkId}/{state}", chunkId, state)
                .accept(MediaType.APPLICATION_JSON)
                .exchange((request, response) -> {
                    log.debug("Response received: {}", response);
                    HttpStatusCode statusCode = response.getStatusCode();
                    if (statusCode.isError() && RETRYABLE_STATE_CODES.contains(statusCode)) {
                        throw new RetryableHttpClientException(statusCode.toString(), statusCode.value());
                    } else {
                        return response.bodyTo(ChunkProcessingData.class);
                    }
                });
    }

    @Recover
    void recover(Exception e, String chunkId, String state) {
        log.error("Unable to retrieve the chunk processing data for: chunkId = {}, state = {}. Reason: {}",
                chunkId, state, e.getMessage()
        );
        throw new DocumentProcessingStateManagerApiException(e.getMessage(), e);
    }
}
