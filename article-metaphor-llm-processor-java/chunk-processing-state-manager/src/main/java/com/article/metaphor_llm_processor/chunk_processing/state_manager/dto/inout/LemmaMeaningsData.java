package com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.inout;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LemmaMeaningsData(@JsonProperty("lemma_meanings") List<LemmaMeanings> lemmaMeanings)
        implements ChunkProcessingData {

    @Override
    public String getState() {
        return "LEMMA_MEANINGS_LOOKUP";
    }
}


