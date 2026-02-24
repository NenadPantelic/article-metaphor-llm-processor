package com.article.metaphor_llm_processor.article_indexer.indexing;

import com.article.metaphor_llm_processor.article_indexer.exception.IndexingException;
import com.article.metaphor_llm_processor.common.model.IndexedDocument;
import com.article.metaphor_llm_processor.common.model.IndexedDocumentChunk;
import com.article.metaphor_llm_processor.common.model.OriginType;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.transformer.splitter.TextSplitter;

import java.net.MalformedURLException;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class DocumentIndexingServiceImplTest {

    // By default, both JUnit 4 and 5 create a new instance of the test class before running each test method.
    // This provides a clean separation of state between tests.
    private final HTMLArticleDocumentReader HTMLArticleDocumentReader = Mockito.mock(HTMLArticleDocumentReader.class);
    private final TextSplitter textSplitter = Mockito.mock(TextSplitter.class);
    private final IndexedDocumentRepository documentRepository = Mockito.mock(IndexedDocumentRepository.class);
    private final IndexedDocumentChunkRepository chunkRepository = Mockito.mock(IndexedDocumentChunkRepository.class);

    private final DocumentIndexingServiceImpl documentIndexingService = new DocumentIndexingServiceImpl(
            HTMLArticleDocumentReader, textSplitter, documentRepository, chunkRepository
    );


    @Test
    public void givenMalformedUrlWhenIndexFromUrlShouldThrowIndexingException() {
        String urlToTest = "abc.xyz.com";
        String testOrigin = "testOrigin";

        Throwable thrown = Assertions.catchThrowable(() -> documentIndexingService.indexFromURL(urlToTest, testOrigin));
        Assertions.assertThat(thrown)
                .isInstanceOf(IndexingException.class)
                .hasCauseInstanceOf(MalformedURLException.class);
    }

    @Test
    public void test() {
        String urlToTest = "https://abc.xyz.com";
        String testOrigin = "testOrigin";

        IndexedDocument expectedDocument = IndexedDocument.builder()
                .id(UUID.randomUUID().toString())
                .name("Test document 1")
                .origin("thetimes")
                .text("Sample test text")
                .type(OriginType.URL)
                .path(urlToTest)
                .build();
        Mockito.doReturn(expectedDocument).when(documentRepository).save(Mockito.any());

        List<IndexedDocumentChunk> chunkList = List.of(
                IndexedDocumentChunk.builder().documentId(expectedDocument.getId()).text("Sample").build(),
                IndexedDocumentChunk.builder().documentId(expectedDocument.getId()).text("test").build(),
                IndexedDocumentChunk.builder().documentId(expectedDocument.getId()).text("text").build()
        );
        Mockito.doReturn(chunkList).when(chunkRepository).saveAll(Mockito.any());

        Assertions.assertThat(documentIndexingService.indexFromURL(urlToTest, testOrigin)).isEqualTo(chunkList);
    }
}