package com.article.metaphor_llm_processor.chunk_processing.state_manager.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Data
@Document("chunk_processing_states")
public class ChunkProcessingState {

    @Id
    private String id;

    @NotBlank
    @Indexed(name = "chunkId_idx", unique = true)
    private String chunkId;

    @NotBlank
    private String documentId;

    @NotBlank
    private String state;

    @Field("data")
    private Map<String, Object> data;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

}
