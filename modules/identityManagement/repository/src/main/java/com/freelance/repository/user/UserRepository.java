package com.freelance.repository.user;

import com.freelance.entity.user.User;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UserRepository extends AbstractRepository<User, UUID> {

    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return executeSingleResultSearch(User.QUERY_FIND_BY_USERNAME, Map.of("username", username));
    }

    public Optional<User> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(get(id));
    }

    public boolean existsByUsername(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return findByUsername(username).isPresent();
    }
}
