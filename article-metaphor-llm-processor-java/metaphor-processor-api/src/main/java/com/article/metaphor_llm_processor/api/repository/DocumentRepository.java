package com.article.metaphor_llm_processor.api.repository;

import com.article.metaphor_llm_processor.api.filter.FilterQueryParser;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class DocumentRepository {

    private static final String COLLECTION_NAME = "documents";
    private static final String[] DOCUMENT_ITEM_FIELDS = {"_id", "name", "origin", "status"};

    private final MongoTemplate mongoTemplate;
    private final FilterQueryParser filterQueryParser;

    public DocumentRepository(MongoTemplate mongoTemplate, FilterQueryParser filterQueryParser) {
        this.mongoTemplate = mongoTemplate;
        this.filterQueryParser = filterQueryParser;
    }

    public List<Document> filter(String filter, String sortBy, String sortDirection, int page, int limit) {
        log.info("Filter: {}", filter);
        Query query = filterQueryParser.parseFilter(filter, sortBy, sortDirection, page, limit, DOCUMENT_ITEM_FIELDS);
        return mongoTemplate.find(query, Document.class, COLLECTION_NAME);
    }

    public Document findById(String id) {
        log.info("Find the document by id {}", id);
        return mongoTemplate.findById(id, Document.class, COLLECTION_NAME);
    }
}
