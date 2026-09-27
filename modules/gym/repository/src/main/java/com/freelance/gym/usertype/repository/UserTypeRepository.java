package com.freelance.gym.usertype.repository;

import com.freelance.gym.usertype.entity.UserTypeEntity;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UserTypeRepository extends AbstractRepository<UserTypeEntity, UUID> {

    public Optional<UserTypeEntity> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return executeSingleResultSearch(UserTypeEntity.QUERY_FIND_BY_CODE, Map.of("code", code.trim().toUpperCase()));
    }

    public List<UserTypeEntity> findAllTypes() {
        return executeListResultSearch(UserTypeEntity.QUERY_FIND_ALL, Map.of());
    }
}
