package com.article.metaphor_llm_processor.article_indexer.model;

import java.time.Instant;


public record DocumentIndexingAttempt(String error,
                                      Instant attemptTime) {
}
