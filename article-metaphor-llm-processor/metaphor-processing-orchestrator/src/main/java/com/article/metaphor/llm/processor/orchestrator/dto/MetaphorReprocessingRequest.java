package com.article.metaphor.llm.processor.orchestrator.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record MetaphorReprocessingRequest(@NotBlank String documentId, List<String> reasons) {
}
