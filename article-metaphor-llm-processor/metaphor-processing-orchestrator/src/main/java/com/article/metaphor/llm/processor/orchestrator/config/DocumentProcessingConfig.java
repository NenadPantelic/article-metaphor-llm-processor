package com.article.metaphor.llm.processor.orchestrator.config;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class DocumentProcessingConfig {

    private static final String PROMPT_TEMPLATE_PATH = "prompt-templates/metaphor-prompt-template.st";
    private static final String PROMPT_TEMPLATE_WITH_DIRECTIVE_PATH = "prompt-templates/metaphor-prompt-template-with-directive.st";

    @Bean
    TextSplitter textSplitter() {
        return new TokenTextSplitter();
    }

    @Bean
    @Qualifier("promptTemplate")
    PromptTemplate promptTemplate() {
        var promptTemplateResource = new ClassPathResource(PROMPT_TEMPLATE_PATH);
        return new PromptTemplate(promptTemplateResource);
    }

    @Bean
    @Qualifier("promptTemplateWithDirective")
    PromptTemplate promptTemplateWithDirective() {
        var promptTemplateResource = new ClassPathResource(PROMPT_TEMPLATE_WITH_DIRECTIVE_PATH);
        return new PromptTemplate(promptTemplateResource);
    }
}
