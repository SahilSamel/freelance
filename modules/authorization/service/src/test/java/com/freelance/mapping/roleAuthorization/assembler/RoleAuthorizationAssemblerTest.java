package com.freelance.mapping.roleAuthorization.assembler;

import com.freelance.authorization.entity.Authorization;
import com.freelance.cache.config.RoleAuthorizationKeyCacheEntry;
import com.freelance.mapping.roleAuthorization.entity.RoleAuthorization;
import com.freelance.role.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoleAuthorizationAssembler Unit Tests")
public class RoleAuthorizationAssemblerTest {

    private final RoleAuthorizationAssembler assembler = new RoleAuthorizationAssembler();

    @Test
    @DisplayName("Should assemble single cache entry from list including nulls and empty lists")
    void testToRoleAuthorizationCacheEntry() {
        RoleAuthorizationKeyCacheEntry empty1 = assembler.toRoleAuthorizationCacheEntry(null);
        assertNotNull(empty1);
        assertTrue(empty1.getAuthorizationKeys().isEmpty());

        RoleAuthorizationKeyCacheEntry empty2 = assembler.toRoleAuthorizationCacheEntry(Collections.emptyList());
        assertNotNull(empty2);
        assertTrue(empty2.getAuthorizationKeys().isEmpty());

        Authorization auth1 = new Authorization(UUID.randomUUID(), "USER_READ", "Read user", "USER", "User Management");
        Authorization auth2 = new Authorization(UUID.randomUUID(), "USER_WRITE", "Write user", "USER", "User Management");
        Role role = new Role(UUID.randomUUID(), "ROLE_ADMIN", "Admin", true);

        RoleAuthorization ra1 = new RoleAuthorization(role, auth1);
        RoleAuthorization ra2 = new RoleAuthorization(role, auth2);
        RoleAuthorization raNull = new RoleAuthorization(role, null);

        RoleAuthorizationKeyCacheEntry entry = assembler.toRoleAuthorizationCacheEntry(List.of(ra1, ra2, raNull));
        assertNotNull(entry);
        assertEquals(2, entry.getAuthorizationKeys().size());
        assertTrue(entry.getAuthorizationKeys().contains("USER_READ"));
        assertTrue(entry.getAuthorizationKeys().contains("USER_WRITE"));
    }

    @Test
    @DisplayName("Should assemble map of role ID to cache entry including nulls and empty maps")
    void testToRoleIdToCacheEntryMap() {
        Map<UUID, RoleAuthorizationKeyCacheEntry> emptyMap1 = assembler.toRoleIdToCacheEntryMap(null);
        assertNotNull(emptyMap1);
        assertTrue(emptyMap1.isEmpty());

        Map<UUID, RoleAuthorizationKeyCacheEntry> emptyMap2 = assembler.toRoleIdToCacheEntryMap(Collections.emptyMap());
        assertNotNull(emptyMap2);
        assertTrue(emptyMap2.isEmpty());

        UUID roleId1 = UUID.randomUUID();
        UUID roleId2 = UUID.randomUUID();
        Role role1 = new Role(roleId1, "ROLE_1", "Role 1", false);
        Role role2 = new Role(roleId2, "ROLE_2", "Role 2", false);

        Authorization auth1 = new Authorization(UUID.randomUUID(), "AUTH_1", "Desc 1", "CAT", "Group");
        Authorization auth2 = new Authorization(UUID.randomUUID(), "AUTH_2", "Desc 2", "CAT", "Group");

        Map<UUID, List<RoleAuthorization>> input = Map.of(
                roleId1, List.of(new RoleAuthorization(role1, auth1)),
                roleId2, List.of(new RoleAuthorization(role2, auth2), new RoleAuthorization(role2, null))
        );

        Map<UUID, RoleAuthorizationKeyCacheEntry> result = assembler.toRoleIdToCacheEntryMap(input);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(roleId1).getAuthorizationKeys().contains("AUTH_1"));
        assertTrue(result.get(roleId2).getAuthorizationKeys().contains("AUTH_2"));
    }
}
