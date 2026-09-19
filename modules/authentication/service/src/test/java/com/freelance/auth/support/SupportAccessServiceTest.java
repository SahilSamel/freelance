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
    @DisplayName("grantAccess sets future expiration time based on configured default duration")
    void testGrantAccess() {
        assertFalse(supportAccessService.isSupportAccessActive());

        SupportGrantResponseDTO grantResponse = supportAccessService.grantAccess();
        assertTrue(grantResponse.active());
        assertNotNull(grantResponse.expiresAt());

        assertTrue(supportAccessService.isSupportAccessActive());
        assertTrue(grantResponse.expiresAt().isAfter(Instant.now().plus(1, ChronoUnit.HOURS)));

        SupportStatusResponseDTO status = supportAccessService.getStatus();
        assertTrue(status.active());
        assertNotNull(status.expiresAt());
        assertTrue(status.expiresAt().isAfter(Instant.now().plus(1, ChronoUnit.HOURS)));
        assertNull(status.pendingReason());
        assertNull(status.pendingTicketId());
    }

    @Test
    @DisplayName("grantAccess respects dynamically configured duration from SystemConfig cache")
    void testGrantAccessDynamicConfig() {
        CacheManager.getInstance().put("cache_SystemConfig", "authentication.support.access.defaultDurationHours", "4");

        SupportGrantResponseDTO grantResponse = supportAccessService.grantAccess();
        assertTrue(grantResponse.active());
        assertTrue(grantResponse.expiresAt().isAfter(Instant.now().plus(3, ChronoUnit.HOURS)));
    }

    @Test
    @DisplayName("revokeAccess immediately clears cache_SupportAccess and terminates the support window")
    void testRevokeAccess() {
        supportAccessService.grantAccess();
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

    @Test
    @DisplayName("grantDay0Access activates support window with explicit expiry timestamp")
    void testGrantDay0Access() {
        Instant future = Instant.now().plus(24, ChronoUnit.HOURS);
        supportAccessService.grantDay0Access(future);

        assertTrue(supportAccessService.isSupportAccessActive());
        SupportStatusResponseDTO status = supportAccessService.getStatus();
        assertTrue(status.active());
        assertEquals(future, status.expiresAt());
    }

    @Test
    @DisplayName("grantAccess falls back to 2 hours when configured duration is invalid (<=0 or >6 or parse exception)")
    void testGrantAccessInvalidDurationFallback() {
        CacheManager.getInstance().put("cache_SystemConfig", "authentication.support.access.defaultDurationHours", "10");

        SupportGrantResponseDTO res = supportAccessService.grantAccess();
        assertTrue(res.active());
        // Should fall back to 2 hours
        assertTrue(res.expiresAt().isBefore(Instant.now().plus(3, ChronoUnit.HOURS)));

        CacheManager.getInstance().put("cache_SystemConfig", "authentication.support.access.defaultDurationHours", "-1");
        SupportGrantResponseDTO res2 = supportAccessService.grantAccess();
        assertTrue(res2.active());

        CacheManager.getInstance().put("cache_SystemConfig", "authentication.support.access.defaultDurationHours", "not_a_number");
        SupportGrantResponseDTO res3 = supportAccessService.grantAccess();
        assertTrue(res3.active());
    }

    @Test
    @DisplayName("SupportNotificationService handles null reason and ticketId safely")
    void testSupportNotificationService() {
        SupportNotificationService notificationService = new SupportNotificationService();
        assertDoesNotThrow(() -> notificationService.notifyAdminOfSupportRequest(null, null));
        assertDoesNotThrow(() -> notificationService.notifyAdminOfSupportRequest("Reason", "TICKET-123"));
    }

    @Test
    @DisplayName("Default 0-arg constructor instantiates successfully")
    void testDefaultConstructor() {
        SupportAccessService service = new SupportAccessService();
        assertNotNull(service);
    }
}
