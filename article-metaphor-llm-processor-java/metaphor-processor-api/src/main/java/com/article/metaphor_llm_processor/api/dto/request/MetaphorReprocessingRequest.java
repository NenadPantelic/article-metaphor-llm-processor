package com.article.metaphor_llm_processor.api.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record MetaphorReprocessingRequest(@NotBlank String documentId, List<String> reasons) {
}