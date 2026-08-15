package com.article.metaphor_llm_processor.orchestrator.dto.processing;

public record MetaphorAnalysis(String expression,
                               int positionStart,
                               int positionEnd,
                               String metaphorType,
                               String explanation) {
}
