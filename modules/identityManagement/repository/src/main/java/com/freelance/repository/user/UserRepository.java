package com.freelance.repository.user;

import com.freelance.entity.user.User;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.Map;
import java.util.UUID;

public class UserRepository extends AbstractRepository<User, UUID> {

    public Object findByUsername(String username) {
        return executeSingleResultSearch("User.findByUsername", Map.of("username", username));
    }

}
