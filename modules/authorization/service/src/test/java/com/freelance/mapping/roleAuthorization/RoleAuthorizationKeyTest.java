package com.freelance.mapping.roleAuthorization;

import com.freelance.mapping.roleAuthorization.entity.RoleAuthorization;
import com.freelance.mapping.roleAuthorization.entity.RoleAuthorizationKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoleAuthorization and RoleAuthorizationKey Tests")
class RoleAuthorizationKeyTest {

    @Test
    @DisplayName("RoleAuthorizationKey getters, setters, equals, and hashCode")
    void testKeyMethods() {
        UUID roleId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();

        RoleAuthorizationKey k1 = new RoleAuthorizationKey(roleId, authId);
        RoleAuthorizationKey k2 = new RoleAuthorizationKey();
        k2.setRoleId(roleId);
        k2.setAuthorizationId(authId);

        assertEquals(roleId, k1.getRoleId());
        assertEquals(authId, k1.getAuthorizationId());
        assertEquals(k1, k2);
        assertEquals(k1.hashCode(), k2.hashCode());
        assertEquals(k1, k1);
        assertNotEquals(k1, null);
        assertNotEquals(k1, "string");

        RoleAuthorizationKey k3 = new RoleAuthorizationKey(UUID.randomUUID(), authId);
        assertNotEquals(k1, k3);
    }

    @Test
    @DisplayName("RoleAuthorization entity constructor and getters")
    void testRoleAuthorizationEntity() {
        UUID roleId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        RoleAuthorizationKey key = new RoleAuthorizationKey(roleId, authId);

        com.freelance.role.entity.Role role = new com.freelance.role.entity.Role(roleId, "TEST_ROLE", "Test Role", false);
        com.freelance.authorization.entity.Authorization auth = new com.freelance.authorization.entity.Authorization(authId, "TEST_AUTH", "Description", "ENDPOINT", "SYSTEM");

        RoleAuthorization ra = new RoleAuthorization(role, auth);
        assertEquals(roleId, ra.getId().getRoleId());
        assertEquals(authId, ra.getId().getAuthorizationId());
        assertEquals(role, ra.getRole());
        assertEquals(auth, ra.getAuthorization());

        RoleAuthorization raEmpty = new RoleAuthorization();
        raEmpty.setId(key);
        assertEquals(key, raEmpty.getId());
    }
}
