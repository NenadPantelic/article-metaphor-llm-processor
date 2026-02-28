package com.article.metaphor_llm_processor.common.model;

public enum DocumentChunkStatus {

    // indexed, but processing has not yet started
    PENDING,
    // the processing has started
    STARTED_PROCESSING,

    // lexical unit processing
    LEXICAL_UNIT_PROCESSING__IN_PROGRESS,
    LEXICAL_UNIT_PROCESSING__COMPLETE,
    LEXICAL_UNIT_PROCESSING__FAILED,

    // dictionary access
    DICTIONARY_ACCESS__IN_PROGRESS,
    DICTIONARY_ACCESS__COMPLETE,
    DICTIONARY_ACCESS__FAILED,

    // metaphor analysis
    METAPHOR_ANALYSIS__IN_PROGRESS,
    METAPHOR_ANALYSIS__COMPLETE,
    METAPHOR_ANALYSIS__FAILED,

    PENDING_REPROCESSING,
    ANOTHER_ATTEMPT_NEEDED,
    PROCESSING_COMPLETE,
    PROCESSING_FAILED
}
