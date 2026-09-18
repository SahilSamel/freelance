package com.freelance.user.repository;

import com.freelance.user.entity.User;
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

    public long countUsers() {
        Long count = em().createNamedQuery(User.QUERY_COUNT_ALL, Long.class).getSingleResult();
        return count != null ? count : 0L;
    }
}
