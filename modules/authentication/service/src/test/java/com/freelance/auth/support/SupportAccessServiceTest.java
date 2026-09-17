package com.freelance.auth.support;

import com.freelance.auth.dto.*;
import com.freelance.cache.config.SupportAccessCacheConfig;
import com.freelance.cache.config.SupportAccessCacheEntry;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.manager.CacheManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SupportAccessServiceTest {

    private SupportAccessService supportAccessService;
    private AtomicBoolean notificationSent;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();

        notificationSent = new AtomicBoolean(false);
        SupportNotificationService notificationService = new SupportNotificationService() {
            @Override
            public void notifyAdminOfSupportRequest(String reason, String ticketId) {
                notificationSent.set(true);
            }
        };
        supportAccessService = new SupportAccessService(notificationService);
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("requestAccess records pending reason and ticket in cache_SupportAccess and triggers notification")
    void testRequestAccess() {
        SupportRequestDTO request = new SupportRequestDTO("Database index rebuild", "INC-55102");
        SupportRequestResponseDTO response = supportAccessService.requestAccess(request);

        assertTrue(response.requested());
        assertTrue(notificationSent.get());

        SupportStatusResponseDTO status = supportAccessService.getStatus();
        assertEquals("Database index rebuild", status.pendingReason());
        assertEquals("INC-55102", status.pendingTicketId());
    }

    @Test
    @DisplayName("grantAccess sets future expiration time in cache_SupportAccess and activates support window")
    void testGrantAccess() {
        assertFalse(supportAccessService.isSupportAccessActive());

        SupportGrantResponseDTO grantResponse = supportAccessService.grantAccess(new SupportGrantRequestDTO(3));
        assertTrue(grantResponse.active());
        assertNotNull(grantResponse.expiresAt());

        assertTrue(supportAccessService.isSupportAccessActive());
        assertTrue(grantResponse.expiresAt().isAfter(Instant.now().plus(2, ChronoUnit.HOURS)));

        SupportStatusResponseDTO status = supportAccessService.getStatus();
        assertTrue(status.active());
        assertNotNull(status.expiresAt());
        assertTrue(status.expiresAt().isAfter(Instant.now().plus(2, ChronoUnit.HOURS)));
        assertNull(status.pendingReason());
        assertNull(status.pendingTicketId());
    }

    @Test
    @DisplayName("revokeAccess immediately clears cache_SupportAccess and terminates the support window")
    void testRevokeAccess() {
        supportAccessService.grantAccess(new SupportGrantRequestDTO(1));
        assertTrue(supportAccessService.isSupportAccessActive());

        SupportRevokeResponseDTO revokeResponse = supportAccessService.revokeAccess();
        assertTrue(revokeResponse.revoked());

        assertFalse(supportAccessService.isSupportAccessActive());

        SupportStatusResponseDTO status = supportAccessService.getStatus();
        assertFalse(status.active());
        assertNull(status.expiresAt());
    }

    @Test
    @DisplayName("isSupportAccessActive returns false when timestamp in cache_SupportAccess is expired")
    void testExpiredAccess() {
        Instant past = Instant.now().minus(10, ChronoUnit.MINUTES);
        SupportAccessCacheEntry entry = new SupportAccessCacheEntry(past, null, null);
        CacheManager.getInstance().put(
                SupportAccessCacheConfig.NAME, SupportAccessCacheConfig.KEY_ACTIVE_WINDOW, entry
        );

        assertFalse(supportAccessService.isSupportAccessActive());
    }
}
