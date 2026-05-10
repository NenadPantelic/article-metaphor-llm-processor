package com.article.metaphor_llm_processor.common.exception;

public class DocumentProcessingStateManagerApiException extends RuntimeException {

    public DocumentProcessingStateManagerApiException() {
    }

    public DocumentProcessingStateManagerApiException(String message) {
        super(message);
    }

    public DocumentProcessingStateManagerApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
