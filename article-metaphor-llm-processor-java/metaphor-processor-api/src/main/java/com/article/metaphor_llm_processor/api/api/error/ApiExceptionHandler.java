package com.article.metaphor_llm_processor.api.api.error;

import com.article.metaphor_llm_processor.api.dto.response.ApiError;
import com.article.metaphor_llm_processor.api.exception.AuthException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.annotation.processing.FilerException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(FilerException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleFilterException(FilerException exception) {
        return new ApiError(exception.getMessage(), HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(FilerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // data not stored properly
    public ApiError handleResultMappingException(FilerException exception) {
        return new ApiError(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(RuntimeException exception) {
        return new ApiError(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError handleException(AuthException authException) {
        return new ApiError(authException.getMessage(), HttpStatus.UNAUTHORIZED.value());
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleException(ValidationException exception) {
        String errMessage = exception.getMessage();
        List<String> errorMessages = null;

        if (exception instanceof ConstraintViolationException) {
            errorMessages = extractViolationsFromException((ConstraintViolationException) exception);
        }

        return new ApiError(errMessage, HttpStatus.BAD_REQUEST.value(), errorMessages);
    }

    private List<String> extractViolationsFromException(ConstraintViolationException exception) {
        return exception.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());
    }
}