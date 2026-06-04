package com.article.metaphor_llm_processor.common.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@AllArgsConstructor
@Builder
@Data
@Document(collection = "document_chunks")
@NoArgsConstructor
public class IndexedDocumentChunk {

    @Id
    private String id;

    @NotBlank
    private String documentId;

    @NotBlank
    private String text;

    @Builder.Default
    private DocumentChunkState state = DocumentChunkState.PENDING_PROCESSING;

    @NotNull
    @Min(1)
    @Builder.Default
    private int order = 1;

    private Instant processingStartedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}