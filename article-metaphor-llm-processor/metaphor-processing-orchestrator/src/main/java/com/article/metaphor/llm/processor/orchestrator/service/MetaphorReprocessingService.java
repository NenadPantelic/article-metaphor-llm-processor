package com.article.metaphor.llm.processor.orchestrator.service;

import com.article.metaphor.llm.processor.orchestrator.dto.MetaphorReprocessingRequest;

public interface MetaphorReprocessingService {

    void handleReprocessingRequest(MetaphorReprocessingRequest metaphorReprocessingRequest);
}
