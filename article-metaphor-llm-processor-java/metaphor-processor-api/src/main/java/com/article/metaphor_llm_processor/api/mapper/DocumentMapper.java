package com.article.metaphor_llm_processor.api.mapper;

import com.article.metaphor_llm_processor.api.dto.response.*;
import com.article.metaphor_llm_processor.api.exception.MappingResultException;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DocumentMapper {

    static final String OBJECT_ID_KEY = "_id";
    static final String NAME_KEY = "name";
    static final String TEXT_KEY = "text";
    static final String STATUS_KEY = "status";
    static final String PATH_KEY = "path";
    static final String TYPE_KEY = "type";
    static final String ORIGIN_KEY = "origin";
    static final String METAPHORS_KEY = "metaphors";

    static final String OFFSET_KEY = "offset";
    static final String PHRASE_KEY = "phrase";
    static final String METAPHOR_TYPE_KEY = "type";
    static final String EXPLANATION_KEY = "explanation";


    public DocumentItem bsonDocumentToDocumentItem(org.bson.Document document) {
        if (document == null) {
            return null;
        }

        return DocumentItem.builder()
                .id(document.get(OBJECT_ID_KEY).toString())
                .status(mapStringToEnum(document.getString(STATUS_KEY), DocumentStatus.class))
                .name(document.getString(NAME_KEY))
                .origin(document.getString(ORIGIN_KEY))
                .build();
    }

    public Document bsonDocumentToDocument(org.bson.Document document) {
        if (document == null) {
            return null;
        }

        List<org.bson.Document> metaphors = (List<org.bson.Document>) document.get(METAPHORS_KEY);
        return Document.builder()
                .id(document.get(OBJECT_ID_KEY).toString())
                .status(mapStringToEnum(document.getString(STATUS_KEY), DocumentStatus.class))
                .text(document.getString(TEXT_KEY))
                .name(document.getString(NAME_KEY))
                .path(document.getString(PATH_KEY))
                .type(document.getString(TYPE_KEY))
                .origin(document.getString(ORIGIN_KEY))
                .metaphors(metaphorDocumentsToMetaphors(metaphors))
                .build();
    }

    public List<DocumentItem> bsonDocumentsToDocumentItems(List<org.bson.Document> documents) {
        if (documents == null) {
            return null;
        }

        return documents.stream()
                .map(this::bsonDocumentToDocumentItem)
                .collect(Collectors.toList());
    }

    private List<Metaphor> metaphorDocumentsToMetaphors(List<org.bson.Document> metaphors) {
        if (metaphors == null) {
            return List.of();
        }

        return metaphors.stream()
                .map(this::metaphorDocumentToMetaphor)
                .sorted(Comparator.comparingInt(Metaphor::offset))
                .collect(Collectors.toList());
    }

    private Metaphor metaphorDocumentToMetaphor(org.bson.Document document) {
        if (document == null) {
            return null;
        }

        String phrase = document.getString(PHRASE_KEY);
        return Metaphor.builder()
                .offset(document.getInteger(OFFSET_KEY))
                .explanation(document.getString(EXPLANATION_KEY))
                .phrase(phrase)
                .type(mapStringToEnum(document.getString(METAPHOR_TYPE_KEY), MetaphorType.class))
                .length(phrase.length())
                .build();
    }

    public <E extends Enum<E>> E mapStringToEnum(String value, Class<E> eClass) {
        try {
            return Enum.valueOf(eClass, value);
        } catch (IllegalArgumentException e) {
            throw new MappingResultException(String.format("Unable to map %s to %s", value, eClass.getSimpleName()));
        }
    }
}
