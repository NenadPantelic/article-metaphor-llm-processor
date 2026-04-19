package com.article.metaphor_llm_processor.api.auth;

import com.article.metaphor_llm_processor.api.dto.internal.identity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class CredentialProvider {

    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

    private static final String CANONICAL_TOKEN_FORMAT = "%s::%s";

    public String get(User user) {
        String token = createToken(user);
        return encode(token);
    }

    private String encode(String canonicalToken) {
        return BASE64_ENCODER.encodeToString(canonicalToken.getBytes(StandardCharsets.UTF_8));
    }

    private String createToken(User user) {
        return String.format(CANONICAL_TOKEN_FORMAT, user.id(), generateNonce());
    }

    private String generateNonce() {
        return BASE64_ENCODER.encodeToString(
                UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)
        );
    }
}
