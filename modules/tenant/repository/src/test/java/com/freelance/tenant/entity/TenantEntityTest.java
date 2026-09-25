package com.freelance.tenant.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantEntity Unit Tests")
class TenantEntityTest {

    @Test
    @DisplayName("Getters, setters, and constructors work correctly")
    void testTenantEntityFields() {
        TenantEntity entity = new TenantEntity("acme", "Acme Corporation", "ACTIVE");
        assertEquals("acme", entity.getTenantId());
        assertEquals("Acme Corporation", entity.getName());
        assertEquals("ACTIVE", entity.getStatus());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());

        entity.setTenantId("new_id");
        entity.setName("New Name");
        entity.setStatus("DEACTIVATED");
        Instant custom = Instant.now().minusSeconds(100);
        entity.setCreatedAt(custom);
        entity.setUpdatedAt(custom);

        assertEquals("new_id", entity.getTenantId());
        assertEquals("New Name", entity.getName());
        assertEquals("DEACTIVATED", entity.getStatus());
        assertEquals(custom, entity.getCreatedAt());
        assertEquals(custom, entity.getUpdatedAt());

        TenantEntity empty = new TenantEntity();
        assertNull(empty.getTenantId());
    }
}
