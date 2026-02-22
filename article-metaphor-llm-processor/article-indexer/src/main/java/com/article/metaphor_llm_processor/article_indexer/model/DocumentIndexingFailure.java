package com.article.metaphor.llm.processor.article_indexer.model;

import com.article.metaphor.llm.processor.common.model.OriginType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Builder
@Data
@org.springframework.data.mongodb.core.mapping.Document(collection = "document_indexing_failures")
@NoArgsConstructor
public class DocumentIndexingFailure {

    @Id
    private String id;

    @NotBlank
    private String source;

    @NotBlank
    private String origin;

    @NotBlank
    private OriginType type;

    @NotBlank
    private Instant lastIndexingAttempt;

    @Builder.Default
    private List<DocumentIndexingAttempt> attempts = new ArrayList<>();

    @Builder.Default
    private DocumentIndexingFailureStatus status = DocumentIndexingFailureStatus.ELIGIBLE_FOR_RETRY;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public void addIndexingAttempt(DocumentIndexingAttempt attempt) {
        attempts.add(attempt);
    }
}
