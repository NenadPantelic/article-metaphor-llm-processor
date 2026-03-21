package com.article.metaphor_llm_processor.chunk_processing.state_manager.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthChecker {

    private static final int OBFUSCATED_SESSION_KEY_LENGTH = 8;
    private static final int PLAINTEXT_SESSION_KEY_LENGTH = 3;
    // one for all client services; for granular access control
    // issue one per client service
    private final String serviceSessionKey;

    public AuthChecker(@Value("${auth.service-session-key}") String serviceSessionKey) {
        this.serviceSessionKey = serviceSessionKey;
    }

    public boolean isAuthenticated(String sessionKey) {
        String obfuscatedSessionKey = obfuscateStr(sessionKey);
        log.debug("Checking session: {}", obfuscateStr(sessionKey));
        boolean authenticated = serviceSessionKey.equals(sessionKey);

        if (authenticated) {
            log.debug("Session {} is valid", obfuscatedSessionKey);
        } else {
            log.debug("Session {} is invalid", obfuscatedSessionKey);
        }

        return authenticated;
    }

    private String obfuscateStr(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        int strLen = value.length();
        return strLen <= PLAINTEXT_SESSION_KEY_LENGTH ?
                "*".repeat(OBFUSCATED_SESSION_KEY_LENGTH) :
                "*".repeat(OBFUSCATED_SESSION_KEY_LENGTH - PLAINTEXT_SESSION_KEY_LENGTH) + value.substring(strLen - PLAINTEXT_SESSION_KEY_LENGTH, strLen);
    }
}
