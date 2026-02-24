package com.article.metaphor_llm_processor.api.config;

import com.article.metaphor_llm_processor.api.auth.AuthHandler;
import com.article.metaphor_llm_processor.api.interceptor.AuthInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@Configuration
//@EnableAspectJAutoProxy
public class InterceptorConfig implements WebMvcConfigurer {

    private final AuthHandler authHandler;
    private final ObjectMapper objectMapper;

    public InterceptorConfig(AuthHandler authHandler, ObjectMapper objectMapper) {
        this.authHandler = authHandler;
        this.objectMapper = objectMapper;
    }

    @Bean
    public AuthInterceptor authInterceptor() {
        return new AuthInterceptor(authHandler, objectMapper);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor())
                .addPathPatterns("/**").excludePathPatterns("/api/v1/auth/login");

    }
}