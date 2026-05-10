package com.article.metaphor_llm_processor.api.service.impl;

import com.article.metaphor_llm_processor.api.auth.identity.Identity;
import com.article.metaphor_llm_processor.api.auth.identity.IdentitySessionContextHolder;
import com.article.metaphor_llm_processor.api.dto.response.IdentityResponse;
import com.article.metaphor_llm_processor.api.service.IdentityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IdentityServiceImpl implements IdentityService {

    @Override
    public IdentityResponse getIdentity() {
        log.info("Resolving an identity from context");
        Identity identity = IdentitySessionContextHolder.get().identity();
        log.info("Resolved identity = {}", identity);
        return new IdentityResponse(identity.username(), identity.role().name());
    }
}
