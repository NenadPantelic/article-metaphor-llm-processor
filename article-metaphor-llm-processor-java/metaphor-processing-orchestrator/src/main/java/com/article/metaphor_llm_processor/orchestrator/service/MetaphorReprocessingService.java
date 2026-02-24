package com.article.metaphor_llm_processor.orchestrator.service;

import com.article.metaphor_llm_processor.orchestrator.dto.reprocessing.MetaphorReprocessingRequest;

public interface MetaphorReprocessingService {

    void handleReprocessingRequest(MetaphorReprocessingRequest metaphorReprocessingRequest);
}
