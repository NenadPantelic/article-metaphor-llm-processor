package com.article.metaphor_llm_processor.common.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

@EnableRetry
@Configuration
public class ClientConfig {

    private final DocumentProcessingStateManagerClientConfigProps clientConfigProps;

    public ClientConfig(DocumentProcessingStateManagerClientConfigProps clientConfigProps) {
        this.clientConfigProps = clientConfigProps;
    }

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.baseUrl(clientConfigProps.url())
                // .defaultHeader("key", "value") // add credentials
                .build();
    }
}
