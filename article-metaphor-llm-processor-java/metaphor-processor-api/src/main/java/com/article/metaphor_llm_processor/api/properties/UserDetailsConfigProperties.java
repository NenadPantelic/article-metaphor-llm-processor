package com.article.metaphor_llm_processor.api.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "users")
public record UserDetailsConfigProperties(String filepath) {
}
