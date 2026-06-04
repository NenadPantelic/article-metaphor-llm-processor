package com.article.metaphor_llm_processor.common.model;

public enum DocumentState {

    // not yet ready, still being indexed
    NOT_READY,
    // not yet processed, waiting for its turn
    PENDING_PROCESSING,
    // being processed at the moment
    PROCESSING,
    // waiting reprocessing
    PENDING_REPROCESSING, // only user can set it to this state
    // being reprocessed at the moment
    REPROCESSING,
    // processing has been done, but some chunks have not been processed (error)
    PROCESSED_INCOMPLETE,

    // completely processed
    PROCESSED_SUCCESSFULLY,

    // failed
    PROCESSED_FAILED
}
