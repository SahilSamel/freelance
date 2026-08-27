package com.freelance.repository.user;

import com.freelance.entity.user.User;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UserRepository extends AbstractRepository<User, UUID> {

    public Optional<User> findByUsername(String username) {
        return executeSingleResultSearch(User.QUERY_FIND_BY_USERNAME, Map.of("username", username));
    }
}
