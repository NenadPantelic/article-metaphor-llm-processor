package com.article.metaphor_llm_processor.chunk_processing.state_manager.filter;

import com.article.metaphor_llm_processor.chunk_processing.state_manager.auth.AuthChecker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class AuthFilter extends OncePerRequestFilter {

    private static final String SESSION_KEY_HEADER = "X-session-key";
    private static final String CLIENT_HEADER = "X-client";

    private static final String APPLICATION_JSON_CONTENT_TYPE = "application/json";

    private static final String UNAUTHENTICATED_REQUEST_ERROR_JSON = "{\"message\": \"Unauthenticated request.\", \"statusCode\": 401}";
    private static final String UNAUTHENTICATED_REQUEST_ERROR_JSON__NO_CLIENT = "{\"message\": \"Unknown client.\", \"statusCode\": 401}";

    private final AuthChecker authChecker;

    public AuthFilter(AuthChecker authChecker) {
        this.authChecker = authChecker;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        log.debug("Authenticating a request...");

        String client = request.getHeader(CLIENT_HEADER);
        if (client == null || client.isEmpty()) {
            log.error("Client is unknown. Show yourself!!!");
            addErrorResponse(response, UNAUTHENTICATED_REQUEST_ERROR_JSON__NO_CLIENT);
            return;
        }

        String sessionKey = request.getHeader(SESSION_KEY_HEADER);
        boolean authenticated = authChecker.isAuthenticated(sessionKey);
        if (!authenticated) {
            log.error("Unauthenticated request!");
            addErrorResponse(response, UNAUTHENTICATED_REQUEST_ERROR_JSON);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void addErrorResponse(HttpServletResponse response, String errorMessage) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(APPLICATION_JSON_CONTENT_TYPE);
        response.getWriter().write(errorMessage);
    }
}
