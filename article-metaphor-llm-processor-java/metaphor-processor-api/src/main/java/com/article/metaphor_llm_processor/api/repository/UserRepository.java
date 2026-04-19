package com.article.metaphor_llm_processor.api.repository;

import com.article.metaphor_llm_processor.api.dto.internal.identity.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(String userId);

    Optional<User> findByUsernameAndPassword(String username, String password);
}
