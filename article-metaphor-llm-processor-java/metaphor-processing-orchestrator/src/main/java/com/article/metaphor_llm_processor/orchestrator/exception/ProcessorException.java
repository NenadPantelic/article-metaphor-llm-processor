package com.article.metaphor_llm_processor.orchestrator.exception;

public class ProcessorException extends RuntimeException {

    public ProcessorException() {
    }

    public ProcessorException(String message) {
        super(message);
    }

    public ProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
