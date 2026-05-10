package com.article.metaphor_llm_processor.api.interceptor;

import com.article.metaphor_llm_processor.api.auth.AuthHandler;
import com.article.metaphor_llm_processor.api.auth.identity.IdentitySessionContextHolder;
import com.article.metaphor_llm_processor.api.auth.identity.IdentitySession;
import com.article.metaphor_llm_processor.api.constant.HttpConstants;
import com.article.metaphor_llm_processor.api.dto.response.ApiError;
import com.article.metaphor_llm_processor.api.exception.AuthException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Slf4j
public class AuthInterceptor implements HandlerInterceptor {


    private final AuthHandler authHandler;

    private final ObjectMapper objectMapper;

    public AuthInterceptor(AuthHandler authHandler,
                           ObjectMapper objectMapper) {
        this.authHandler = authHandler;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String authorization = request.getHeader(HttpConstants.AUTHORIZATION_HEADER);
        if (authorization == null || authorization.isBlank()) {
            setUnauthorizedResponse(response);
            return false;
        }

        String token = extractToken(authorization);
        try {
            IdentitySession identitySession = authHandler.resolve(token);
            IdentitySessionContextHolder.set(identitySession);
            return true;
        } catch (AuthException e) {
            setUnauthorizedResponse(response);
            return false;
        } catch (Exception e) {
            setErrorResponse(response, e, HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        IdentitySessionContextHolder.clear();
    }

    private String extractToken(String authorization) {
        try {
            return authorization.substring(HttpConstants.BEARER.length());
        } catch (StringIndexOutOfBoundsException e) {
            log.warn("Could not extract the authorization token");
            return null;
        }
    }

    private void setUnauthorizedResponse(HttpServletResponse response) throws IOException {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        setErrorResponse(response, status.getReasonPhrase(), status);
    }

    private void setErrorResponse(HttpServletResponse response,
                                  Exception e,
                                  HttpStatus status) throws IOException {
        String errorMessage = (e instanceof AuthException) ? e.getMessage() : status.getReasonPhrase();
        setErrorResponse(response, errorMessage, status);
    }

    private void setErrorResponse(HttpServletResponse response,
                                  String errorMessage,
                                  HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");

        ApiError apiError = new ApiError(errorMessage, status.value());
        try {
            String json = objectMapper.writeValueAsString(apiError);
            response.getWriter().write(json);
        } catch (IOException ioException) {
            log.error("Unexpected error occurred", ioException);
            throw ioException;
        }
    }
}
