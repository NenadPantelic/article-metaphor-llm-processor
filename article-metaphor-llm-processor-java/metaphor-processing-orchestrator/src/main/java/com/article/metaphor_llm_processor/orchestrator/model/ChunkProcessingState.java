package com.article.metaphor_llm_processor.orchestrator.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@Document("chunk_processing_states")
public class ChunkProcessingState {

    @Id
    private String id;

    @NotBlank
    @Indexed(name = "chunkId_idx", unique = true)
    private String chunkId;

    @NotBlank
    private ProcessingMilestone reachedMilestone;

    private Instant lastExecutionTimestamp;

    private boolean failedOnLastExecution;

    @Builder.Default
    private List<ChunkProcessingError> errors = new ArrayList<>();

    // if the state record is inactive, it cannot be used in processing
    @Builder.Default
    private boolean active = true;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public void addError(ChunkProcessingError error) {
        errors.add(error);
    }

    public void deactivate() {
        setActive(false);
    }
}
