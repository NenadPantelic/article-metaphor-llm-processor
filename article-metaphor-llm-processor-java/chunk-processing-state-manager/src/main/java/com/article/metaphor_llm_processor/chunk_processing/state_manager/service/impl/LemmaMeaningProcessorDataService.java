package com.article.metaphor_llm_processor.chunk_processing.state_manager.service.impl;

import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.inout.ChunkProcessingData;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.inout.LemmaMeanings;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.inout.LemmaMeaningsData;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.model.ChunkProcessingState;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.repository.ChunkProcessingStateRepository;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.service.ChunkProcessingDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LemmaMeaningProcessorDataService extends ChunkProcessingDataService {

    private static final String LEMMA_MEANINGS_ATTR_KEY = "lemma_meanings";

    protected LemmaMeaningProcessorDataService(ChunkProcessingStateRepository chunkProcessingStateRepository) {
        super(chunkProcessingStateRepository);
    }

    @Override
    public void updateChunkProcessingStateData(ChunkProcessingState chunkProcessingState,
                                               ChunkProcessingData chunkProcessingData) {
        LemmaMeaningsData lemmaMeaningsData = (LemmaMeaningsData) chunkProcessingData;
        Map<String, Object> data = chunkProcessingState.getData();
        Map<String, Object> lexicalUnitProcessingAttributes = Map.of(
                LEMMA_MEANINGS_ATTR_KEY,
                lemmaMeaningsData.lemmaMeanings()
        );
        data.put(lemmaMeaningsData.getState(), lexicalUnitProcessingAttributes);
    }

    @Override
    protected ChunkProcessingData mapToProcessingState(ChunkProcessingState chunkProcessingState) {
        Map<String, Object> data = chunkProcessingState.getData();
        List<Object> meaningObjects = (List<Object>) data.get(LEMMA_MEANINGS_ATTR_KEY);

        List<LemmaMeanings> lemmaMeanings = meaningObjects.stream()
                .map(LemmaMeanings.class::cast)
                .toList();
        return new LemmaMeaningsData(lemmaMeanings);
    }

}

