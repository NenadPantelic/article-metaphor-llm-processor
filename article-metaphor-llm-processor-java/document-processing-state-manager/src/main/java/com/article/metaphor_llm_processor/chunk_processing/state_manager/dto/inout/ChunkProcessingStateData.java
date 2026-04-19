package com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.inout;

import com.article.metaphor_llm_processor.common.dto.processing.ChunkProcessingData;
import com.article.metaphor_llm_processor.common.dto.processing.inout.LemmaMeaningsData;
import com.article.metaphor_llm_processor.common.dto.processing.inout.LexicalUnitProcessingData;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

public record ChunkProcessingStateData(@JsonProperty("chunk_id") String chunkId,
                                       @JsonProperty("document_id") String documentId,
                                       @JsonTypeInfo(
                                               use = JsonTypeInfo.Id.NAME,
                                               include = JsonTypeInfo.As.PROPERTY, // default, just for transparency, do not remove
                                               property = "type")
                                       @JsonSubTypes({
                                               @JsonSubTypes.Type(value = LexicalUnitProcessingData.class, name = "lexical_processing_data"),
                                               @JsonSubTypes.Type(value = LemmaMeaningsData.class, name = "lemma_meanings_data")
                                       })
                                       @JsonProperty("data") ChunkProcessingData data) {


}
