package com.article.metaphor_llm_processor.common.model;

import lombok.Getter;

@Getter
public enum DocumentChunkState {

    // indexed, but processing has not yet started
    PENDING_PROCESSING,
    // the processing has started
    PROCESSING,
    // attempt failed, reprocess again
    REPROCESSING_NEEDED,
    // user requested its reprocessing
    REPROCESSING_REQUESTED,
    // reprocessing
    REPROCESSING,
    COMPLETED,
    FAILED
}
