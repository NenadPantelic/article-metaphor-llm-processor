package com.article.metaphor_llm_processor.chunk_processing.state_manager.service.impl;

import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.inout.ChunkProcessingData;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.inout.LexicalUnitProcessingData;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.model.ChunkProcessingState;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.repository.ChunkProcessingStateRepository;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.service.ChunkProcessingDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LexicalUnitProcessingDataService extends ChunkProcessingDataService {

    private static final String LEXICAL_UNITS_ATTR_KEY = "lexical_units";
    private static final String UNIQUE_LEMMAS_ATTR_KEY = "unique_lemmas";

    protected LexicalUnitProcessingDataService(ChunkProcessingStateRepository chunkProcessingStateRepository) {
        super(chunkProcessingStateRepository);
    }

    @Override
    public void updateChunkProcessingStateData(ChunkProcessingState chunkProcessingState,
                                               ChunkProcessingData chunkProcessingData) {
        LexicalUnitProcessingData lexicalUnitProcessingData = (LexicalUnitProcessingData) chunkProcessingData;
        Map<String, Object> data = chunkProcessingState.getData();

        Map<String, Object> lexicalUnitProcessingAttributes = Map.of(
                LEXICAL_UNITS_ATTR_KEY, lexicalUnitProcessingData.lexicalUnits(),
                UNIQUE_LEMMAS_ATTR_KEY, lexicalUnitProcessingData.uniqueLemmas()
        );
        data.put(lexicalUnitProcessingData.getState(), lexicalUnitProcessingAttributes);
    }

    @Override
    protected ChunkProcessingData mapToProcessingState(ChunkProcessingState chunkProcessingState) {
        Map<String, Object> data = chunkProcessingState.getData();

        return new LexicalUnitProcessingData(
                (List<Map<String, String>>) data.get(LEXICAL_UNITS_ATTR_KEY),
                (List<Map<String, String>>) data.get(UNIQUE_LEMMAS_ATTR_KEY)
        );
    }

}
