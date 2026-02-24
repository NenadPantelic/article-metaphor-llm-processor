package com.article.metaphor_llm_processor.orchestrator.configproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "processing")
public record ProcessingConfigProperties(int maxRetry,
                                         int intervalInMillis,
                                         String lexicalUnitProcessingExchange) {

}
