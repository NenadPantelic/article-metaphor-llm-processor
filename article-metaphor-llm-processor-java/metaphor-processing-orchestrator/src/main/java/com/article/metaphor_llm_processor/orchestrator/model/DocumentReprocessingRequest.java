package com.article.metaphor_llm_processor.orchestrator.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@Builder
@Data
@Document(collection = "documentReprocessingRequests")
@NoArgsConstructor
public class DocumentReprocessingRequest {

    @Id
    private String id;

    @NotBlank
    @Indexed(name = "documentId_idx", unique = true)
    private String documentId;

    @NotBlank
    private List<String> reasons;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
