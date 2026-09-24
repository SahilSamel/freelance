package com.freelance.auth.support;

import com.freelance.ConfigurationEngine;
import com.freelance.auth.dto.*;
import com.freelance.auth.support.validation.SupportRequestValidation;
import com.freelance.cache.config.SupportAccessCacheConfig;
import com.freelance.cache.config.SupportAccessCacheEntry;
import com.freelance.keys.authentication.support.SupportConfigKeys;
import com.freelance.manager.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class SupportAccessService {

    private static final Logger logger = LoggerFactory.getLogger(SupportAccessService.class);

    private final SupportNotificationService notificationService;

    public SupportAccessService() {
        this(new SupportNotificationService());
    }

    public SupportAccessService(SupportNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private SupportAccessCacheEntry getEntry() {
        try {
            SupportAccessCacheEntry entry = CacheManager.get(
                    SupportAccessCacheConfig.NAME, SupportAccessCacheConfig.KEY_ACTIVE_WINDOW
            );
            if (entry == null) {
                entry = new SupportAccessCacheEntry();
                CacheManager.put(
                        SupportAccessCacheConfig.NAME, SupportAccessCacheConfig.KEY_ACTIVE_WINDOW, entry
                );
            }
            return entry;
        } catch (Exception e) {
            logger.debug("Cache access fallback: {}", e.getMessage());
            return new SupportAccessCacheEntry();
        }
    }

    private void saveEntry(SupportAccessCacheEntry entry) {
        try {
            CacheManager.put(
                    SupportAccessCacheConfig.NAME, SupportAccessCacheConfig.KEY_ACTIVE_WINDOW, entry
            );
        } catch (Exception e) {
            logger.debug("Cache save bypassed: {}", e.getMessage());
        }
    }

    public boolean isSupportAccessActive() {
        try {
            SupportAccessCacheEntry entry = CacheManager.get(
                    SupportAccessCacheConfig.NAME, SupportAccessCacheConfig.KEY_ACTIVE_WINDOW
            );
            return entry != null && entry.isActive();
        } catch (Exception e) {
            return false;
        }
    }

    public SupportRequestResponseDTO requestAccess(SupportRequestDTO request) {
        new SupportRequestValidation().validateRequest(request);

        String safeReason = request.reason().trim();
        String safeTicket = request.ticketId().trim();

        SupportAccessCacheEntry entry = getEntry();
        entry.setPendingReason(safeReason);
        entry.setPendingTicket(safeTicket);
        saveEntry(entry);

        notificationService.notifyAdminOfSupportRequest(safeReason, safeTicket);

        return new SupportRequestResponseDTO(true, "Support access request dispatched to client administrator.");
    }

    public SupportGrantResponseDTO grantAccess() {
        int durationHours;
        try {
            durationHours = ConfigurationEngine.getInt(SupportConfigKeys.SUPPORT_ACCESS_DEFAULT_DURATION_HOURS);
            if (durationHours <= 0 || durationHours > 6) {
                durationHours = 2;
            }
        } catch (Exception e) {
            durationHours = 2;
        }

        Instant expiresAt = Instant.now().plus(durationHours, ChronoUnit.HOURS);

        SupportAccessCacheEntry entry = getEntry();
        entry.setExpiresAt(expiresAt);
        entry.setPendingReason(null);
        entry.setPendingTicket(null);
        saveEntry(entry);

        logger.info("[SupportAccessService] Support access granted for {} hour(s) until {}", durationHours, expiresAt);
        return new SupportGrantResponseDTO(true, expiresAt);
    }

    public void grantDay0Access(Instant expiresAt) {
        SupportAccessCacheEntry entry = getEntry();
        entry.setExpiresAt(expiresAt);
        entry.setPendingReason(null);
        entry.setPendingTicket(null);
        saveEntry(entry);
        logger.info("[SupportAccessService] Day-0 Support access granted until {}", expiresAt);
    }

    public SupportRevokeResponseDTO revokeAccess() {
        SupportAccessCacheEntry entry = getEntry();
        entry.clear();
        saveEntry(entry);

        logger.info("[SupportAccessService] Support access has been REVOKED immediately by client administrator.");
        return new SupportRevokeResponseDTO(true);
    }

    public SupportStatusResponseDTO getStatus() {
        SupportAccessCacheEntry entry = getEntry();
        boolean active = entry.isActive();
        Instant expiresAt = entry.getExpiresAt();

        return new SupportStatusResponseDTO(
                active,
                expiresAt,
                (entry.getPendingReason() != null && !entry.getPendingReason().isBlank()) ? entry.getPendingReason() : null,
                (entry.getPendingTicket() != null && !entry.getPendingTicket().isBlank()) ? entry.getPendingTicket() : null
        );
    }
}
