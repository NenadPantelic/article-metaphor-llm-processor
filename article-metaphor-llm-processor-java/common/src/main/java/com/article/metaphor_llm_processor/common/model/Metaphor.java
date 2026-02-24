package com.article.metaphor_llm_processor.common.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Metaphor {

    @EqualsAndHashCode.Include
    @NotBlank
    private final String chunkId;

    @EqualsAndHashCode.Include
    @Min(0)
    private final int offset;

    // metaphor phrase
    @NotBlank
    private final String phrase;
    private final MetaphorType type;
    private final String explanation;
    @CreatedDate
    private final Instant createdAt;
    @LastModifiedDate
    private final Instant updatedAt;
}
