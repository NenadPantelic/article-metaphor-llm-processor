package com.article.metaphor_llm_processor.api.repository;

import com.article.metaphor_llm_processor.api.properties.UserDetailsConfigProperties;
import com.article.metaphor_llm_processor.api.dto.internal.identity.Role;
import com.article.metaphor_llm_processor.api.dto.internal.identity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Slf4j
@Component
public class UserRepositoryImpl implements UserRepository {

    private static final String COMMA_DELIMITER = ",";

    private final Map<String, User> usersByIdMap;
    private final Map<String, User> usersByUsernameMap;

    public UserRepositoryImpl(UserDetailsConfigProperties userDetailsConfigProperties) {
        usersByIdMap = new HashMap<>();
        usersByUsernameMap = new HashMap<>();
        loadUserEntries(userDetailsConfigProperties.filepath());
    }

    @Override
    public Optional<User> findById(String userId) {
        return Optional.ofNullable(usersByIdMap.get(userId));
    }

    @Override
    public Optional<User> findByUsernameAndPassword(String username, String password) {
        User user = usersByUsernameMap.get(username);
        if (user == null || !user.password().equals(password)) {
            return Optional.empty();
        }

        return Optional.of(user);
    }

    private void loadUserEntries(String filepath) {
        log.info("Loading user entries...");
        int lineNo = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (lineNo == 1) {
                    continue; // header
                }

                // displayName,username,password,role
                String[] values = line.split(COMMA_DELIMITER);

                User user = User.builder()
                        .id(UUID.randomUUID().toString())
                        .displayName(values[0])
                        .username(values[1])
                        .password(values[2])
                        .role(Role.valueOf(values[3]))
                        .build();

                usersByIdMap.put(user.id(), user);
                usersByUsernameMap.put(user.username(), user);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (usersByIdMap.isEmpty()) {
            throw new RuntimeException("No users loaded in. At least one user is needed...");
        }

        log.info("Loaded {} user entries", usersByIdMap.size());
    }
}
