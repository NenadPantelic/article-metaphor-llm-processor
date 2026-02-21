package com.article.metaphor.llm.processor.orchestrator.configproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "processing")
public record ProcessingConfigProperties(int maxRetry,
                                         int intervalInMillis,
                                         String reprocessingQueue) {

}
