package com.freelance.authorization.seeder;

import com.freelance.authorization.SystemRoles;
import com.freelance.entity.authorization.Authorization;
import com.freelance.entity.mapping.roleAuthorization.RoleAuthorization;
import com.freelance.entity.mapping.roleAuthorization.RoleAuthorizationKey;
import com.freelance.entity.role.Role;
import com.freelance.repository.authorization.AuthorizationRepository;
import com.freelance.repository.mapping.roleAuthorization.RoleAuthorizationRepository;
import com.freelance.repository.role.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Synchronizes code-defined {@link com.freelance.rest.app.endpoint.annotation.Authorization} annotations
 * and system roles into the database at application startup using the repository layer.
 * <p>
 * Performs full 2-way reconciliation:
 * <ul>
 *     <li>Inserts new permissions, system roles, and mappings.</li>
 *     <li>Updates changed metadata (names, descriptions, group paths).</li>
 *     <li>Deletes stale authorizations, system roles, and mappings that were removed from code.</li>
 *     <li>Bypasses database writes entirely if no changes are detected.</li>
 * </ul>
 * </p>
 */
public class AuthorizationSeeder {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationSeeder.class);

    private static final int BATCH_SIZE = 100;
    private final AuthorizationRepository authorizationRepo;
    private final RoleRepository roleRepo;
    private final RoleAuthorizationRepository roleAuthorizationRepo;

    /**
     * Constructs an {@link AuthorizationSeeder} with default repository dependencies.
     */
    public AuthorizationSeeder() {
        this(new AuthorizationRepository(), new RoleRepository(), new RoleAuthorizationRepository());
    }

    public AuthorizationSeeder(
            AuthorizationRepository authorizationRepo,
            RoleRepository roleRepo,
            RoleAuthorizationRepository roleAuthorizationRepo
    ) {
        this.authorizationRepo = authorizationRepo;
        this.roleRepo = roleRepo;
        this.roleAuthorizationRepo = roleAuthorizationRepo;
    }

    public void seed() {
        List<com.freelance.rest.app.endpoint.annotation.Authorization> annotations =
                AuthorizationRegistry.getAllAuthorizations();

        if (annotations.isEmpty()) {
            logger.info("[AuthorizationSeeder] No @Authorization annotations discovered on classpath.");
            return;
        }

        Map<String, Authorization> authEntityMap = syncAuthorizations(annotations);
        Map<String, Role> roleEntityMap = syncSystemRoles(annotations);
        syncRoleAuthorizationMappings(annotations, authEntityMap, roleEntityMap);

        logger.info("[AuthorizationSeeder] Authorization database synchronization completed successfully.");
    }

    private Map<String, Authorization> syncAuthorizations(
            List<com.freelance.rest.app.endpoint.annotation.Authorization> annotations
    ) {
        Map<String, Authorization> existingAuthMap = authorizationRepo.findAllAsMap();
        Set<String> codeKeys = new HashSet<>();

        List<Authorization> toInsert = new ArrayList<>();
        List<Authorization> toUpdate = new ArrayList<>();

        for (com.freelance.rest.app.endpoint.annotation.Authorization authAnn : annotations) {
            String key = authAnn.key();
            codeKeys.add(key);
            Authorization existing = existingAuthMap.get(key);

            if (existing == null) {
                Authorization newAuth = new Authorization(
                        UUID.randomUUID(),
                        key,
                        authAnn.name(),
                        authAnn.description(),
                        authAnn.group()
                );
                toInsert.add(newAuth);
            } else {
                boolean changed = !Objects.equals(existing.getName(), authAnn.name())
                        || !Objects.equals(existing.getDescription(), authAnn.description())
                        || !Objects.equals(existing.getGroupPath(), authAnn.group());

                if (changed) {
                    existing.setName(authAnn.name());
                    existing.setDescription(authAnn.description());
                    existing.setGroupPath(authAnn.group());
                    toUpdate.add(existing);
                }
            }
        }

        List<Authorization> toDelete = new ArrayList<>();
        for (Map.Entry<String, Authorization> entry : existingAuthMap.entrySet()) {
            if (!codeKeys.contains(entry.getKey())) {
                toDelete.add(entry.getValue());
            }
        }

        if (!toInsert.isEmpty()) {
            authorizationRepo.persistEntriesInBatch(toInsert, BATCH_SIZE);
            logger.info("[AuthorizationSeeder] Inserted {} new authorization(s).", toInsert.size());
        }

        if (!toUpdate.isEmpty()) {
            authorizationRepo.mergeEntriesInBatch(toUpdate, BATCH_SIZE);
            logger.info("[AuthorizationSeeder] Updated metadata for {} authorization(s).", toUpdate.size());
        }

        if (!toDelete.isEmpty()) {
            authorizationRepo.removeEntriesInBatch(toDelete, BATCH_SIZE);
            logger.info("[AuthorizationSeeder] Removed {} stale authorization(s) no longer in code.", toDelete.size());
        }

        if (toInsert.isEmpty() && toUpdate.isEmpty() && toDelete.isEmpty()) {
            logger.debug("[AuthorizationSeeder] Authorizations are up to date. 0 DB modifications required.");
            return existingAuthMap;
        }

        return authorizationRepo.findAllAsMap();
    }

    private Map<String, Role> syncSystemRoles(
            List<com.freelance.rest.app.endpoint.annotation.Authorization> annotations
    ) {
        Set<String> codeRoleNames = new HashSet<>();
        for (com.freelance.rest.app.endpoint.annotation.Authorization authAnn : annotations) {
            if (authAnn.role() != null && authAnn.role() != SystemRoles.NONE) {
                codeRoleNames.add(authAnn.role().name());
            }
        }

        Map<String, Role> existingRolesMap = roleRepo.findAllSystemRolesAsMap();
        List<Role> rolesToInsert = new ArrayList<>();
        List<Role> rolesToDelete = new ArrayList<>();

        for (String roleName : codeRoleNames) {
            if (!existingRolesMap.containsKey(roleName)) {
                UUID roleId = UUID.nameUUIDFromBytes(("role:" + roleName).getBytes(StandardCharsets.UTF_8));
                Role role = new Role(roleId, roleName, "System generated role for " + roleName, true);
                rolesToInsert.add(role);
            }
        }

        for (Map.Entry<String, Role> entry : existingRolesMap.entrySet()) {
            if (!codeRoleNames.contains(entry.getKey())) {
                rolesToDelete.add(entry.getValue());
            }
        }

        if (!rolesToInsert.isEmpty()) {
            roleRepo.persistEntriesInBatch(rolesToInsert, BATCH_SIZE);
            logger.info("[AuthorizationSeeder] Inserted {} new system role(s).", rolesToInsert.size());
        }

        if (!rolesToDelete.isEmpty()) {
            roleRepo.removeEntriesInBatch(rolesToDelete, BATCH_SIZE);
            logger.info("[AuthorizationSeeder] Removed {} stale system role(s).", rolesToDelete.size());
        }

        if (rolesToInsert.isEmpty() && rolesToDelete.isEmpty()) {
            logger.debug("[AuthorizationSeeder] System roles are up to date. 0 DB modifications required.");
            return existingRolesMap;
        }

        return roleRepo.findAllSystemRolesAsMap();
    }

    private void syncRoleAuthorizationMappings(
            List<com.freelance.rest.app.endpoint.annotation.Authorization> annotations,
            Map<String, Authorization> authEntityMap,
            Map<String, Role> roleEntityMap
    ) {
        Map<RoleAuthorizationKey, RoleAuthorization> existingMappings =
                roleAuthorizationRepo.findAllSystemRoleMappings();

        Map<RoleAuthorizationKey, RoleAuthorization> expectedMappings = new HashMap<>();

        for (com.freelance.rest.app.endpoint.annotation.Authorization authAnn : annotations) {
            if (authAnn.role() != null && authAnn.role() != SystemRoles.NONE) {
                String roleName = authAnn.role().name();
                Role role = roleEntityMap.get(roleName);
                Authorization authEntity = authEntityMap.get(authAnn.key());

                if (role != null && authEntity != null) {
                    RoleAuthorizationKey key = new RoleAuthorizationKey(role.getId(), authEntity.getId());
                    expectedMappings.put(key, new RoleAuthorization(role, authEntity));
                }
            }
        }

        List<RoleAuthorization> mappingsToInsert = new ArrayList<>();
        for (Map.Entry<RoleAuthorizationKey, RoleAuthorization> entry : expectedMappings.entrySet()) {
            if (!existingMappings.containsKey(entry.getKey())) {
                mappingsToInsert.add(entry.getValue());
            }
        }

        List<RoleAuthorization> mappingsToDelete = new ArrayList<>();
        for (Map.Entry<RoleAuthorizationKey, RoleAuthorization> entry : existingMappings.entrySet()) {
            if (!expectedMappings.containsKey(entry.getKey())) {
                mappingsToDelete.add(entry.getValue());
            }
        }

        if (mappingsToInsert.isEmpty() && mappingsToDelete.isEmpty()) {
            logger.debug("[AuthorizationSeeder] System role-authorization mappings are up to date. 0 DB modifications required.");
            return;
        }

        if (!mappingsToDelete.isEmpty()) {
            roleAuthorizationRepo.removeEntriesInBatch(mappingsToDelete, BATCH_SIZE);
            logger.info("[AuthorizationSeeder] Removed {} stale role-authorization mapping(s).", mappingsToDelete.size());
        }

        if (!mappingsToInsert.isEmpty()) {
            roleAuthorizationRepo.persistEntriesInBatch(mappingsToInsert, BATCH_SIZE);
            logger.info("[AuthorizationSeeder] Inserted {} new role-authorization mapping(s).", mappingsToInsert.size());
        }
    }
}
