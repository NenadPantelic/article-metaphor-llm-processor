package com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.inout;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record LexicalUnitProcessingData(@JsonProperty("lexical_units") List<Map<String, String>> lexicalUnits,
                                        @JsonProperty("unique_lemmas") List<Map<String, String>> uniqueLemmas) implements ChunkProcessingData {

    @Override
    public String getState() {
        return "LEXICAL_UNIT_PROCESSING";
    }
}
