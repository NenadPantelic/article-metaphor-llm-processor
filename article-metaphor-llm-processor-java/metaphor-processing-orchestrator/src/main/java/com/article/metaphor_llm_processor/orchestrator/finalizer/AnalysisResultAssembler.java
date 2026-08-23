package com.article.metaphor_llm_processor.orchestrator.finalizer;

import com.article.metaphor_llm_processor.common.model.*;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentChunkRepository;
import com.article.metaphor_llm_processor.common.repository.IndexedDocumentRepository;
import com.article.metaphor_llm_processor.orchestrator.dto.processing.ArticleMetaphorAnalysis;
import com.article.metaphor_llm_processor.orchestrator.dto.processing.MetaphorAnalysis;
import com.article.metaphor_llm_processor.orchestrator.model.ChunkProcessingState;
import com.article.metaphor_llm_processor.orchestrator.model.ProcessingMilestone;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class AnalysisResultAssembler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final IndexedDocumentRepository documentRepository;
    private final IndexedDocumentChunkRepository chunkRepository;

    public AnalysisResultAssembler(IndexedDocumentRepository documentRepository,
                                   IndexedDocumentChunkRepository chunkRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    public void assembleAnalysisResults(String documentId) {

        List<ChunkProcessingState> chunkProcessingStates = new ArrayList<>(); // TODO
        Optional<IndexedDocument> documentOptional = documentRepository.findById(documentId);

        if (documentOptional.isEmpty()) {
            log.error("Could not find a document with id '{}'", documentId);
            return;
        }
        IndexedDocument document = documentOptional.get();
        List<AnalyzedTextSegment> analyzedTextSegments = new ArrayList<>();
        int cumulativeTextLength = 0;

        for (ChunkProcessingState state : chunkProcessingStates) {
            Optional<IndexedDocumentChunk> indexedDocumentChunkOptional = chunkRepository.findById(state.getChunkId());
            if (indexedDocumentChunkOptional.isEmpty()) {
                log.error("Could not find a chunk with id '{}'", state.getChunkId());
                return;
            }

            IndexedDocumentChunk chunk = indexedDocumentChunkOptional.get();
            Map<String, Object> processingData = state.getData();
            Map<String, Object> metaphorAnalysisData = (Map<String, Object>) processingData.get(
                    ProcessingMilestone.METAPHOR_ANALYSIS.name()
            );
            ArticleMetaphorAnalysis articleMetaphorAnalysis = convertMapToArticleMetaphorAnalysis(metaphorAnalysisData);

            log.info("Assembling metaphor results of document '{}'", documentId);
            List<Pair<Integer, Integer>> metaphorPositions = new ArrayList<>();
            Map<Pair<Integer, Integer>, MetaphorAnalysis> metaphorPosToMetaphorsMap = new HashMap<>();

            for (var metaphorAnalysis : articleMetaphorAnalysis.metaphors()) {
                Pair<Integer, Integer> positionPair = Pair.of(
                        metaphorAnalysis.positionStart(), metaphorAnalysis.positionEnd()
                );

                metaphorPositions.add(positionPair);
                metaphorPosToMetaphorsMap.put(positionPair, metaphorAnalysis);
            }

            addMissingPositionIntervals(metaphorPositions, cumulativeTextLength);
            metaphorPositions.sort(Comparator.comparingInt(Pair::getLeft));


            String text = document.getText();
            for (var metPosition : metaphorPositions) {
                if (!metaphorPositions.contains(metPosition)) {
                    int startPosition = metPosition.getLeft();
                    int endPosition = metPosition.getRight();
                    String segmentText = text.substring(startPosition, endPosition + 1);
                    analyzedTextSegments.add(new AnalyzedTextSegment(segmentText, null));
                } else {
                    MetaphorAnalysis metaphorAnalysis = metaphorPosToMetaphorsMap.get(metPosition);
                    MetaphorMetadata metaphorMetadata = new MetaphorMetadata(
                            MetaphorType.valueOf(metaphorAnalysis.metaphorType()),
                            metaphorAnalysis.explanation()
                    );
                    analyzedTextSegments.add(new AnalyzedTextSegment(metaphorAnalysis.expression(), metaphorMetadata));
                }
            }

            cumulativeTextLength += chunk.getText().length();
        }

        document.setAnalyzedTextSegments(analyzedTextSegments);
        documentRepository.save(document);
    }

    private void addMissingPositionIntervals(List<Pair<Integer, Integer>> positions, int textLength) {
        int firstPosition = 0, nextPosition = 0;
        int noOfPositions = positions.size();

        for (int i = 0; i < noOfPositions; i++) {
            var metPosition = positions.get(i);
            nextPosition = metPosition.getLeft();

            if (firstPosition != nextPosition - 1) {
                positions.add(Pair.of(firstPosition, nextPosition - 1));
            }

            firstPosition = metPosition.getRight() + 1;
        }

        nextPosition = textLength;
        if (firstPosition != nextPosition) {
            positions.add(Pair.of(firstPosition, nextPosition - 1));
        }
    }

    private ArticleMetaphorAnalysis convertMapToArticleMetaphorAnalysis(Map<String, Object> metaphorAnalysis) {
        return OBJECT_MAPPER.convertValue(metaphorAnalysis, ArticleMetaphorAnalysis.class);
    }
}
