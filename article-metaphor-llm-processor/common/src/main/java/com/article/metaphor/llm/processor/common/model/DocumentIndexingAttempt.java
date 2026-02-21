package com.article.metaphor.llm.processor.common.model;

import java.time.Instant;


public record DocumentIndexingAttempt(String error,
                                      Instant attemptTime) {
}
