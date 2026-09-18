package com.freelance.auth.bootstrap;

import com.freelance.ConfigurationEngine;
import com.freelance.auth.dto.Day0BootstrapResult;
import com.freelance.auth.support.SupportAccessService;
import com.freelance.auth.vendor.VendorSupportKeyUtility;
import com.freelance.cache.config.SystemConfigurationCache;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateRequestDTO;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateResponseDTO;
import com.freelance.configuration.system.SystemConfigurationService;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.authentication.AuthenticationConfigKeys;
import com.freelance.manager.CacheManager;
import com.freelance.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.PublicKey;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SupportBootstrapServiceTest {

    private AtomicLong userCount;
    private UserRepository userRepository;
    private SupportAccessService supportAccessService;
    private AtomicReference<String> savedConfigValue;
    private SystemConfigurationService fakeConfigService;
    private SupportBootstrapService bootstrapService;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();

        userCount = new AtomicLong(0L);
        userRepository = new UserRepository() {
            @Override
            public long countUsers() {
                return userCount.get();
            }
        };
        supportAccessService = new SupportAccessService();
        savedConfigValue = new AtomicReference<>(null);

        fakeConfigService = new SystemConfigurationService() {
            @Override
            public SystemConfigurationUpdateResponseDTO updateConfiguration(SystemConfigurationUpdateRequestDTO request) {
                savedConfigValue.set(request.configValue());
                try {
                    CacheManager.getInstance().put(SystemConfigurationCache.NAME, request.configId(), request.configValue());
                } catch (Exception ignored) {}
                return new SystemConfigurationUpdateResponseDTO(request.configId(), request.configValue(), request.description());
            }
        };

        bootstrapService = new SupportBootstrapService(userRepository, fakeConfigService, supportAccessService);
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Day-0 Fresh Boot: Generates keypair, persists public key, opens support window, mints valid token")
    void testBootstrap_Day0FreshInstallation() {
        // Condition: unconfigured public key and 0 users
        Day0BootstrapResult result = bootstrapService.bootstrapIfRequired();

        assertTrue(result.bootstrapped());
        assertNotNull(result.publicKeyBase64());
        assertNotNull(result.privateKeyPem());
        assertNotNull(result.token());
        assertNotNull(result.expiresAt());
        assertTrue(result.expiresAt().isAfter(Instant.now()));

        // Check that public key was updated in FL_SYSTEM_CONFIGURATION
        assertEquals(result.publicKeyBase64(), savedConfigValue.get());

        // Check that support access window is active
        assertTrue(supportAccessService.isSupportAccessActive());

        // Verify the generated token with the public key
        PublicKey publicKey = VendorSupportKeyUtility.parsePublicKey(result.publicKeyBase64());
        Claims claims = VendorSupportKeyUtility.verifyAndParseSupportToken(result.token(), publicKey);

        assertEquals("vendor-support", claims.getIssuer());
        assertEquals(com.freelance.system.SystemHardcoding.VENDOR_SUPPORT_USER_ID.asString(), claims.getSubject());
        assertEquals(com.freelance.system.SystemHardcoding.VENDOR_SUPPORT_SESSION_KEY.asString(), claims.get("sessionKey"));
    }

    @Test
    @DisplayName("Day-1+ Configured Key: Skips bootstrap if vendor public key is already configured")
    void testBootstrap_KeyAlreadyConfigured_Skips() {
        // Pre-configure public key in cache
        try {
            CacheManager.getInstance().put(
                    SystemConfigurationCache.NAME,
                    AuthenticationConfigKeys.VENDOR_SUPPORT_PUBLIC_KEY.getKey(),
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA..."
            );
        } catch (Exception ignored) {}

        Day0BootstrapResult result = bootstrapService.bootstrapIfRequired();

        assertFalse(result.bootstrapped());
        assertNull(result.publicKeyBase64());
        assertNull(result.privateKeyPem());
        assertNull(result.token());
        assertNull(savedConfigValue.get());
        assertFalse(supportAccessService.isSupportAccessActive());
    }

    @Test
    @DisplayName("Unconfigured Key with Existing Users: Logs warning and skips bootstrap without minting tokens")
    void testBootstrap_KeyMissingWithExistingUsers_LogsWarningAndSkips() {
        userCount.set(1L);

        Day0BootstrapResult result = bootstrapService.bootstrapIfRequired();

        assertFalse(result.bootstrapped());
        assertNull(result.publicKeyBase64());
        assertNull(result.privateKeyPem());
        assertNull(result.token());
        assertNull(savedConfigValue.get());
        assertFalse(supportAccessService.isSupportAccessActive());
    }
}
