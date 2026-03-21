package com.article.metaphor_llm_processor.common.exception;

import java.util.List;

public record ErrorReport(String message, int status, List<String> errorMessages) {

    public static ErrorReport BAD_REQUEST = new ErrorReport("Bad request.", 400, List.of());
    public static ErrorReport NOT_FOUND = new ErrorReport("Not found.", 404, List.of());
    public static ErrorReport INTERNAL_SERVER_ERROR = new ErrorReport("Internal server error.", 500, List.of());

    public ErrorReport withMessage(String message) {
        return new ErrorReport(message, this.status, this.errorMessages);
    }

    public ErrorReport withMessage(List<String> errorMessages) {
        return new ErrorReport(this.message, this.status, errorMessages);
    }

    public ErrorReport withMessage(String message, List<String> errorMessages) {
        return new ErrorReport(message, this.status, errorMessages);
    }
}
