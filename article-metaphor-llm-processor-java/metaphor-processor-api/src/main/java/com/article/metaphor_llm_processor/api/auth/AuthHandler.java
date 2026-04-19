package com.article.metaphor_llm_processor.api.auth;

import com.article.metaphor_llm_processor.api.auth.identity.Identity;
import com.article.metaphor_llm_processor.api.auth.identity.IdentitySession;
import com.article.metaphor_llm_processor.api.exception.AuthException;
import com.article.metaphor_llm_processor.api.dto.internal.identity.User;
import com.article.metaphor_llm_processor.api.properties.AuthConfigProperties;
import com.article.metaphor_llm_processor.api.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class AuthHandler {


    private final UserRepository userRepository;
    private final CredentialProvider credentialProvider;
    private final Cache<String, IdentitySession> identitySessionCache;
    private final long expirationTime;

    public AuthHandler(UserRepository userRepository,
                       CredentialProvider credentialProvider,
                       Cache<String, IdentitySession> identitySessionCache,
                       AuthConfigProperties authConfigProperties) {
        this.userRepository = userRepository;
        this.credentialProvider = credentialProvider;
        this.identitySessionCache = identitySessionCache;
        this.expirationTime = authConfigProperties.expirationTimeInMinutes();
    }

    public String authenticate(String username, String password) {
        log.info("Authenticating user {}...", username);
        User user = userRepository.findByUsernameAndPassword(username, password)
                .orElseThrow(() -> new AuthException("Invalid credential."));
        String credential = credentialProvider.get(user);
        Identity identity = new Identity(user.id(), user.username(), user.role());
        Instant sessionExpirationTime = getSessionExpirationTime();
        identitySessionCache.put(credential, new IdentitySession(identity, credential, sessionExpirationTime));
        return credential;
    }

    public IdentitySession resolve(String credential) {
        if (credential == null || credential.isBlank()) {
            throw new AuthException("Invalid credential.");
        }

        IdentitySession identitySession = identitySessionCache.getIfPresent(credential);
        if (identitySession == null) {
            throw new AuthException("Invalid credential.");
        }

        if (!Instant.now().isBefore(identitySession.expirationTime())) {
            identitySessionCache.invalidate(credential);
            throw new AuthException("Expired credentials.");
        }

        return identitySession;
    }

    public void clearAuthentication(String credential) {
        if (credential != null) {
            // TODO
            identitySessionCache.invalidate(credential);
        }
    }

    private Instant getSessionExpirationTime() {
        return Instant.now().plus(Duration.of(expirationTime, ChronoUnit.MINUTES));
    }
}
