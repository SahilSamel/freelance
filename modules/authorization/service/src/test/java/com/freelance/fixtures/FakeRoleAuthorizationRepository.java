package com.freelance.fixtures;

import com.freelance.entity.mapping.roleAuthorization.RoleAuthorization;
import com.freelance.entity.mapping.roleAuthorization.RoleAuthorizationKey;
import com.freelance.repository.mapping.roleAuthorization.RoleAuthorizationRepository;

import java.util.*;
import java.util.stream.Collectors;

public class FakeRoleAuthorizationRepository extends RoleAuthorizationRepository {
    public final Map<RoleAuthorizationKey, RoleAuthorization> storage = new HashMap<>();
    public int persistBatchCalls = 0;
    public int removeBatchCalls = 0;

    @Override
    public List<RoleAuthorization> getRoleAuthorizationsByRoleId(UUID roleId) {
        if (roleId == null) return Collections.emptyList();
        return storage.values().stream()
                .filter(ra -> roleId.equals(ra.getId().getRoleId()))
                .toList();
    }

    @Override
    public Map<UUID, List<RoleAuthorization>> getAllMappingsGroupedByRoleId() {
        return storage.values().stream()
                .collect(Collectors.groupingBy(ra -> ra.getId().getRoleId()));
    }

    @Override
    public void persistEntriesInBatch(List<RoleAuthorization> entities, int... batchSize) {
        if (entities != null) {
            persistBatchCalls += entities.size();
            for (RoleAuthorization ra : entities) {
                storage.put(ra.getId(), ra);
            }
        }
    }

    @Override
    public void removeEntriesInBatch(List<RoleAuthorization> entities, int... batchSize) {
        if (entities != null) {
            removeBatchCalls += entities.size();
            for (RoleAuthorization ra : entities) {
                storage.remove(ra.getId());
            }
        }
    }

    @Override
    public void removeByRoleId(UUID roleId) {
        if (roleId == null) return;
        List<RoleAuthorization> toRemove = getRoleAuthorizationsByRoleId(roleId);
        removeEntriesInBatch(toRemove);
    }
}
