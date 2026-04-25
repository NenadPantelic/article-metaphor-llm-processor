package com.article.metaphor_llm_processor.orchestrator.configproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "housekeeping")
public record HousekeepingConfigProperties(int intervalInMillis) {

}
