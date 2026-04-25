package com.article.metaphor_llm_processor.common.exception;

import lombok.Getter;

@Getter
public class RetryableHttpClientException extends RuntimeException {

    private final int statusCode;

    public RetryableHttpClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}
