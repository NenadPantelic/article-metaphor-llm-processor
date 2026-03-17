package com.article.metaphor_llm_processor.common.exception;

import lombok.AllArgsConstructor;

import java.util.ArrayList;

@AllArgsConstructor
public class ApiException extends RuntimeException {

    private final ErrorReport errorReport;

    public ApiException(String message, int status) {
        this(new ErrorReport(message, status, new ArrayList<>()));
    }
}
