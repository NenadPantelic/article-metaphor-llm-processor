package com.article.metaphor_llm_processor.orchestrator.producer;

import com.article.metaphor_llm_processor.orchestrator.configproperties.ProcessingConfigProperties;
import com.article.metaphor_llm_processor.orchestrator.dto.PipelineMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChunkProcessingMessageProducer {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public ChunkProcessingMessageProducer(RabbitTemplate rabbitTemplate,
                                          ProcessingConfigProperties processingConfigProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = processingConfigProperties.lexicalUnitProcessingExchange();
    }

    public void sendMessage(PipelineMessage pipelineMessage) {
        log.info("Sending {}...", pipelineMessage);
        rabbitTemplate.convertAndSend(exchange, pipelineMessage);
    }
}
