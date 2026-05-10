package com.article.metaphor_llm_processor.common.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "document-state-manager-client")
public record DocumentProcessingStateManagerClientConfigProps(String url,
                                                              int maxRetry,
                                                              int delay,
                                                              float multiplier,
                                                              int maxDelay) {
}
