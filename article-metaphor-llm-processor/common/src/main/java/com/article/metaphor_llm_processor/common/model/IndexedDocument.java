package com.article.metaphor.llm.processor.common.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@Builder
@Data
@Document(collection = "documents")
@NoArgsConstructor
@CompoundIndex(name = "source_origin_idx", def = "{'origin': 1, 'origin': 1}")
public class IndexedDocument {

    @Id
    private String id;

    @NotBlank
    private String name;

    @NotBlank
    private String text;

    @NotBlank
    private String origin;

    @NotBlank
    private String path;

    @NotBlank
    private OriginType type;

    @Builder.Default
    private DocumentStatus status = DocumentStatus.NOT_READY;

    @Builder.Default
    private Set<Metaphor> metaphors = new HashSet<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public void addMetaphors(Set<Metaphor> metaphors) {
        this.metaphors.addAll(metaphors);
    }

    public void clearAllMetaphors() {
        metaphors.clear();
    }
}
