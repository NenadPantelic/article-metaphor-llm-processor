package com.article.metaphor_llm_processor.api.service;

import com.article.metaphor_llm_processor.api.dto.request.MetaphorReprocessingRequest;

public interface MetaphorReprocessingService {

    void handleReprocessingRequest(MetaphorReprocessingRequest metaphorReprocessingRequest);
}
