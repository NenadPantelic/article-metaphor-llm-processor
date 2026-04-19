package com.article.metaphor_llm_processor.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record DocumentFilterRequest(@NotBlank String filter,
                                    String sortBy,
                                    String sortOrder,
                                    Integer page,
                                    Integer limit) {
}
