package com.article.metaphor_llm_processor.common.dto.processing.inout;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LemmaMeanings(@JsonProperty("lemma") String lemma,
                            @JsonProperty("cambridge_explanations") List<String> cambridgeExplanations,
                            @JsonProperty("ldoce_explanations") List<String> ldoceExplanations) {
}
