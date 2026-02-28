package com.article.metaphor_llm_processor.chunk_processing.state_manager.service.impl;

import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.response.ChunkProcessingStateInfo;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.model.ChunkProcessingState;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.repository.ChunkProcessingStateRepository;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.service.ChunkProcessingInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LexicalUnitProcessingInfoService extends ChunkProcessingInfoService {


    protected LexicalUnitProcessingInfoService(ChunkProcessingStateRepository chunkProcessingStateRepository) {
        super(chunkProcessingStateRepository);
    }

    @Override
    protected ChunkProcessingStateInfo mapToProcessingInfo(ChunkProcessingState chunkProcessingState) {
        return null;
    }
}
