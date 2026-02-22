package com.article.metaphor.llm.processor.article_indexer.indexing;

import com.article.metaphor.llm.processor.common.model.IndexedDocumentChunk;

import java.util.List;

public interface DocumentIndexingService {

    /**
     * Indexes a document by retrieving the content from the given URL.
     *
     * @param source   a path (URL in this case) of the indexing document
     * @param origin an origin (the filesystem or the network identity) which owns the resource
     */
    List<IndexedDocumentChunk> indexFromURL(String source, String origin);

    // TODO: later an indexing from the file could be added
}
