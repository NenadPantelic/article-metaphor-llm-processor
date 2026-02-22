package com.article.metaphor.llm.processor.article_indexer.model;

import java.time.Instant;


public record DocumentIndexingAttempt(String error,
                                      Instant attemptTime) {
}
