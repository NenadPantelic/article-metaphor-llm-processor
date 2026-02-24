package com.article.metaphor_llm_processor.article_indexer.configproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "indexing")
public record IndexingConfigProperties(int maxRetry,
                                       int retryIntervalInMillis,
                                       String queue) {

}
