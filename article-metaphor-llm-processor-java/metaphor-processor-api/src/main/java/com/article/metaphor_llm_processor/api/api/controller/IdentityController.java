package com.article.metaphor_llm_processor.api.api.controller;

import com.article.metaphor_llm_processor.api.dto.response.IdentityResponse;
import com.article.metaphor_llm_processor.api.service.IdentityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/identity")
@Validated
public class IdentityController {

    private final IdentityService identityService;

    public IdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    public ResponseEntity<IdentityResponse> whoAmI() {
        log.info("Received a 'whoami' request");
        return ResponseEntity.ok(identityService.getIdentity());
    }
}
