package com.freelance.mapping.userRole;

import com.freelance.mapping.userRole.entity.UserRoleEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserRoleEntity and Pk Tests")
class UserRoleEntityTest {

    @Test
    @DisplayName("UserRoleEntity getters and setters")
    void testGettersAndSetters() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        UserRoleEntity entity = new UserRoleEntity();
        entity.setUserId(userId);
        entity.setRoleId(roleId);

        assertEquals(userId, entity.getUserId());
        assertEquals(roleId, entity.getRoleId());

        UserRoleEntity entity2 = new UserRoleEntity(userId, roleId);
        assertEquals(userId, entity2.getUserId());
        assertEquals(roleId, entity2.getRoleId());
    }

    @Test
    @DisplayName("UserRoleEntity.Pk equals and hashCode")
    void testPkEqualsAndHashCode() {
        UUID u1 = UUID.randomUUID();
        UUID r1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();

        UserRoleEntity.Pk pk1 = new UserRoleEntity.Pk(u1, r1);
        UserRoleEntity.Pk pk2 = new UserRoleEntity.Pk(u1, r1);
        UserRoleEntity.Pk pk3 = new UserRoleEntity.Pk(u2, r1);
        UserRoleEntity.Pk emptyPk = new UserRoleEntity.Pk();

        assertEquals(pk1, pk2);
        assertEquals(pk1.hashCode(), pk2.hashCode());
        assertNotEquals(pk1, pk3);
        assertNotEquals(pk1, null);
        assertNotEquals(pk1, "someString");
        assertEquals(pk1, pk1);
        assertNotNull(emptyPk);
    }
}
