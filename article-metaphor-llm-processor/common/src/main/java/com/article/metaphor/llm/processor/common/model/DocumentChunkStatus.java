package com.article.metaphor.llm.processor.common.model;

public enum DocumentChunkStatus {

    PENDING,
    PROCESSING,
    PENDING_REPROCESSING,
    SUCCESSFULLY_PROCESSED,
    NEXT_ATTEMPT_NEEDED,
    FAILED_TO_PROCESS
}
