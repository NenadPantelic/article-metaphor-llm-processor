package com.article.metaphor_llm_processor.api.config;

import com.article.metaphor_llm_processor.api.auth.identity.IdentitySession;
import com.article.metaphor_llm_processor.api.properties.AuthConfigProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class IdentityCacheConfig {

    private final AuthConfigProperties authConfigProperties;

    public IdentityCacheConfig(AuthConfigProperties authConfigProperties) {
        this.authConfigProperties = authConfigProperties;
    }

    @Bean
    public Cache<String, IdentitySession> identitySessionCache() {
        return Caffeine.newBuilder()
                .maximumSize(authConfigProperties.cacheSize())
                .expireAfterWrite(authConfigProperties.expirationTimeInMinutes(), TimeUnit.MINUTES)
                .build();
    }
}
