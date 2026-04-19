package com.article.metaphor_llm_processor.common.model;

import lombok.Getter;

@Getter
public enum DocumentChunkStatus {

    // indexed, but processing has not yet started
    PENDING(ProcessingMilestone.NONE),
    // the processing has started
    STARTED_PROCESSING(ProcessingMilestone.NONE),

    // lexical unit processing
    LEXICAL_UNIT_PROCESSING__PENDING(ProcessingMilestone.LEXICAL_UNIT_PROCESSING), // for reprocessing
    LEXICAL_UNIT_PROCESSING__IN_PROGRESS(ProcessingMilestone.LEXICAL_UNIT_PROCESSING),
    LEXICAL_UNIT_PROCESSING__COMPLETE(ProcessingMilestone.LEXICAL_UNIT_PROCESSING),
    LEXICAL_UNIT_PROCESSING__FAILED(ProcessingMilestone.LEXICAL_UNIT_PROCESSING),

    // dictionary access
    DICTIONARY_ACCESS__PENDING(ProcessingMilestone.DICTIONARY_ACCESS),
    DICTIONARY_ACCESS__IN_PROGRESS(ProcessingMilestone.DICTIONARY_ACCESS),
    DICTIONARY_ACCESS__COMPLETE(ProcessingMilestone.DICTIONARY_ACCESS),
    DICTIONARY_ACCESS__FAILED(ProcessingMilestone.DICTIONARY_ACCESS),

    // metaphor analysis
    METAPHOR_ANALYSIS__PENDING(ProcessingMilestone.METAPHOR_ANALYSIS),
    METAPHOR_ANALYSIS__IN_PROGRESS(ProcessingMilestone.METAPHOR_ANALYSIS),
    METAPHOR_ANALYSIS__COMPLETE(ProcessingMilestone.METAPHOR_ANALYSIS),
    METAPHOR_ANALYSIS__FAILED(ProcessingMilestone.METAPHOR_ANALYSIS),

    PENDING_REPROCESSING(ProcessingMilestone.NONE),
    ANOTHER_ATTEMPT_NEEDED(ProcessingMilestone.NONE),
    PROCESSING_COMPLETE(ProcessingMilestone.DONE),
    PROCESSING_FAILED(ProcessingMilestone.DONE);

    private final ProcessingMilestone milestone;

    DocumentChunkStatus(ProcessingMilestone milestone) {
        this.milestone = milestone;
    }
}
