package com.freelance.alerts.whatsapp;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertDirection;
import com.freelance.alerts.AlertMessageStatus;
import com.freelance.alerts.LogicalAlertAction;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.channel.repository.AlertChannelConfigRepository;
import com.freelance.alerts.message.entity.AlertMessageEntity;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.ConfigurationEngine;
import com.freelance.consumer.alerts.outbound.whatsapp.WhatsAppErrorCode;
import com.freelance.event.model.EventPriority;
import com.freelance.keys.alerts.AlertConfigKeys;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.user.read.UserReadRequestDTO;
import com.freelance.user.read.UserReadResponseDTO;
import com.freelance.user.read.UserReadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility providing gatekeeping checks, queue state management, Meta error code
 * mapping and resolution, opt-in/opt-out keyword detection, sliding human support
 * session tracking, and user identity verification.
 */
public final class WhatsAppUtility {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppUtility.class);
    private static final AtomicBoolean queuePaused = new AtomicBoolean(false);

    private static final Pattern KEYWORD_CLEANUP_PATTERN = Pattern.compile("^[.\\s,!]+|[.\\s,!]+$");

    private static final Set<String> DEFAULT_OPT_OUT_KEYWORDS = Set.of(
            "STOP", "STOPALL", "UNSUBSCRIBE", "CANCEL", "END", "QUIT", "OPTOUT", "OPT OUT", "OPT-OUT", "REVOKE", "OFF"
    );
    private static final Set<String> DEFAULT_OPT_IN_KEYWORDS = Set.of(
            "START", "UNSTOP", "SUBSCRIBE", "OPTIN", "OPT IN", "OPT-IN", "RESUME", "CONTINUE", "RESTART", "YES", "ON"
    );

    public record ErrorResolution(
            String normalizedCode,
            String description,
            LogicalAlertAction action,
            boolean isRetriable
    ) {}

    private WhatsAppUtility() {
        // utility class
    }

    // =========================================================================
    // Queue Gatekeeping & State Management
    // =========================================================================

    public static boolean isQueuePaused() {
        return queuePaused.get();
    }

    public static void pauseQueue() {
        queuePaused.set(true);
        logger.warn("[WhatsAppUtility] Outbound dispatch queue has been PAUSED due to critical Meta failure.");
    }

    public static void resumeQueue() {
        queuePaused.set(false);
        logger.info("[WhatsAppUtility] Outbound dispatch queue has been RESUMED.");
    }

    public static void checkQueueNotPaused() {
        if (isQueuePaused()) {
            throw new IllegalStateException("Outbound queue is currently paused due to Meta account/billing issue.");
        }
    }

    // =========================================================================
    // Sending Eligibility & Precedence Gatekeeping
    // =========================================================================

    /**
     * Gatekeeping check to determine if an alert can be sent to WhatsApp.
     * Evaluates in strict precedence:
     * 1. Global Queue / Channel availability
     * 2. Contact existence and opt-out / active status
     * 3. Active sliding human session (HIGH priority bypasses)
     *
     * @param contact           user contact entity (nullable)
     * @param priority          event priority
     * @param channelConfigRepo optional channel config repository (nullable)
     * @return true if all conditions are satisfied to dispatch; false otherwise
     */
    public static boolean canSendMessage(
            UserContactEntity contact,
            EventPriority priority,
            AlertChannelConfigRepository channelConfigRepo
    ) {
        if (isQueuePaused()) {
            return false;
        }
        if (channelConfigRepo != null && !channelConfigRepo.isChannelAvailable(AlertChannel.WHATSAPP)) {
            return false;
        }
        if (contact == null || contact.isOptedOut() || !contact.isActive()) {
            return false;
        }
        return !isHumanSessionActive(contact, priority);
    }

    public static boolean canSendMessage(UserContactEntity contact, EventPriority priority) {
        return canSendMessage(contact, priority, null);
    }

    public static boolean canSendMessage(UserContactEntity contact) {
        return canSendMessage(contact, EventPriority.NORMAL, null);
    }

    // =========================================================================
    // Sliding Human Session Management
    // =========================================================================

    /**
     * Checks whether an active human support session window is currently running.
     * High priority events (e.g. OTP, security alerts) bypass this window.
     */
    public static boolean isHumanSessionActive(UserContactEntity contact, EventPriority priority) {
        if (contact == null || priority == EventPriority.HIGH) {
            return false;
        }
        return contact.isHumanSessionActive();
    }

    /**
     * Checks whether an interactive 24-hour customer session window is active.
     * Evaluates:
     * 1. Contact's human_session_expires_at (if active)
     * 2. Or if an INBOUND message was received from the user on WHATSAPP within the last 24 hours.
     */
    public static boolean isInteractiveSessionActive(UserContactEntity contact, AlertMessageRepository messageRepository) {
        if (contact == null) {
            return false;
        }
        if (contact.isHumanSessionActive()) {
            return true;
        }
        if (messageRepository != null) {
            try {
                Instant cutoff = Instant.now().minus(Duration.ofHours(24));
                return messageRepository.findLatestByUserAndChannel(contact.getUserId(), AlertChannel.WHATSAPP)
                        .filter(m -> m.getDirection() == AlertDirection.INBOUND)
                        .map(m -> m.getTimestamp() != null && m.getTimestamp().isAfter(cutoff))
                        .orElse(false);
            } catch (Exception e) {
                logger.warn("[WhatsAppUtility] Unable to check latest inbound message for interactive session: {}", e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * Extends or initiates a sliding human support session for a contact.
     */
    public static void extendHumanSession(UserContactEntity contact, UserContactRepository contactRepo, Duration duration) {
        if (contact == null || contactRepo == null) return;
        contact.setHumanSessionExpiresAt(Instant.now().plus(duration));
        contactRepo.merge(contact);
        logger.info("[WhatsAppUtility] Extended human session for contact {} until {}",
                contact.getContactValue(), contact.getHumanSessionExpiresAt());
    }

    // =========================================================================
    // Identity Verification
    // =========================================================================

    public static boolean isUserValid(UUID userId, UserReadService userReadService) {
        if (userReadService == null || userId == null) {
            return false;
        }
        try {
            UserReadResponseDTO response = userReadService.readUser(new UserReadRequestDTO(userId, null));
            return response != null && response.userDTO() != null;
        } catch (ConstraintException e) {
            logger.info("[WhatsAppUtility] User ID {} not found in user service: {}", userId, e.getMessage());
            return false;
        } catch (ValidationException e) {
            logger.warn("[WhatsAppUtility] Invalid user ID {} passed to user service: {}", userId, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("[WhatsAppUtility] Failed to verify user existence for userId {}", userId, e);
            return false;
        }
    }

    // =========================================================================
    // Keyword Matching (Inbound Opt-in / Opt-out)
    // =========================================================================

    private static String normalizeKeyword(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return KEYWORD_CLEANUP_PATTERN.matcher(text.trim().toUpperCase(Locale.ROOT)).replaceAll("");
    }

    public static Set<String> getOptOutKeywords() {
        String raw = ConfigurationEngine.get(AlertConfigKeys.WHATSAPP_OPT_OUT_KEYWORDS);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_OPT_OUT_KEYWORDS;
        }
        return Arrays.stream(raw.split(","))
                .map(WhatsAppUtility::normalizeKeyword)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<String> getOptInKeywords() {
        String raw = ConfigurationEngine.get(AlertConfigKeys.WHATSAPP_OPT_IN_KEYWORDS);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_OPT_IN_KEYWORDS;
        }
        return Arrays.stream(raw.split(","))
                .map(WhatsAppUtility::normalizeKeyword)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public static boolean isOptOutKeyword(String text) {
        String normalized = normalizeKeyword(text);
        return !normalized.isEmpty() && getOptOutKeywords().contains(normalized);
    }

    public static boolean isOptInKeyword(String text) {
        String normalized = normalizeKeyword(text);
        return !normalized.isEmpty() && getOptInKeywords().contains(normalized);
    }

    public static void updateUserConsent(
            UUID userId,
            String contactValue,
            UserContactRepository contactRepo,
            boolean isOptedOut,
            boolean isActive
    ) {
        if (contactRepo == null) return;
        Optional<UserContactEntity> contactOpt = Optional.empty();
        if (userId != null) {
            contactOpt = contactRepo.findByUserAndChannel(userId, AlertChannel.WHATSAPP);
        }
        if (contactOpt.isEmpty() && contactValue != null && !contactValue.isBlank()) {
            contactOpt = contactRepo.findByContactAndChannel(contactValue.trim(), AlertChannel.WHATSAPP);
        }

        if (contactOpt.isPresent()) {
            UserContactEntity contact = contactOpt.get();
            contact.setOptedOut(isOptedOut);
            contact.setActive(isActive);
            contactRepo.merge(contact);
            logger.info("[WhatsAppUtility] Updated contact consent for user {} (phone={}): isOptedOut={}, isActive={}",
                    contact.getUserId(), contact.getContactValue(), isOptedOut, isActive);
        } else if (userId != null && contactValue != null && !contactValue.isBlank()) {
            UserContactEntity newContact = new UserContactEntity(
                    UUID.randomUUID(),
                    userId,
                    AlertChannel.WHATSAPP,
                    contactValue.trim(),
                    isOptedOut,
                    isActive
            );
            contactRepo.persist(newContact);
            logger.info("[WhatsAppUtility] Created new contact with consent for user {} (phone={}): isOptedOut={}, isActive={}",
                    userId, contactValue.trim(), isOptedOut, isActive);
        }
    }

    // =========================================================================
    // Meta Error Mapping & Action Application
    // =========================================================================

    public static ErrorResolution resolveMetaError(int metaErrorCode, Integer metaErrorSubcode, String metaMessage) {
        WhatsAppErrorCode error = WhatsAppErrorCode.fromMeta(metaErrorCode, metaErrorSubcode);
        String code = (error == WhatsAppErrorCode.UNKNOWN)
                ? "ERR_META_UNKNOWN_" + metaErrorCode
                : error.getCode();
        String description = (metaMessage != null && !metaMessage.isBlank())
                ? metaMessage
                : error.getDescription();
        return new ErrorResolution(code, description, error.getAction(), error.isRetriable());
    }

    public static void applyFailureAction(
            String metaMessageId,
            int metaErrorCode,
            Integer metaErrorSubcode,
            String rawDetails,
            AlertMessageRepository messageRepo,
            UserContactRepository contactRepo,
            AlertTemplateRepository templateRepo
    ) {
        if (metaMessageId == null || messageRepo == null) {
            return;
        }

        ErrorResolution resolution = resolveMetaError(metaErrorCode, metaErrorSubcode, rawDetails);
        logger.warn("[WhatsAppUtility] Processing failure: wamid={}, code={}, action={}",
                metaMessageId, resolution.normalizedCode(), resolution.action());

        Optional<AlertMessageEntity> messageOpt = messageRepo.findByMetaMessageId(metaMessageId);
        if (messageOpt.isPresent()) {
            AlertMessageEntity msg = messageOpt.get();
            msg.setStatus(resolution.isRetriable() ? AlertMessageStatus.PENDING : AlertMessageStatus.FAILED);
            msg.setErrorCode(resolution.normalizedCode());
            msg.setRawErrorDetails(rawDetails);
            messageRepo.merge(msg);

            UUID userId = msg.getUserId();

            switch (resolution.action()) {
                case DISABLE_USER_CHANNEL -> updateUserConsent(userId, null, contactRepo, false, false);
                case MARK_USER_OPTED_OUT -> updateUserConsent(userId, null, contactRepo, true, false);
                case TRIGGER_HUMAN_INTERVENTION -> {
                    if (contactRepo != null && userId != null) {
                        contactRepo.findByUserAndChannel(userId, AlertChannel.WHATSAPP)
                                .ifPresent(c -> extendHumanSession(c, contactRepo, Duration.ofHours(24)));
                    }
                }
                case CRITICAL_ADMIN_ALERT -> {
                    pauseQueue();
                    logger.error("================================================================================");
                    logger.error("[CRITICAL WHATSAPP ALERT] Meta Fatal Error: {} ({})", resolution.normalizedCode(), resolution.description());
                    logger.error("================================================================================");
                }
                case PAUSE_TEMPLATE -> {
                    if (msg.getTemplate() != null && templateRepo != null) {
                        AlertTemplateEntity tpl = msg.getTemplate();
                        tpl.setStatus(TemplateStatus.PAUSED);
                        templateRepo.merge(tpl);
                        logger.warn("[WhatsAppUtility] Template {} paused by Meta for message {}", tpl.getTemplateKey(), metaMessageId);
                    } else {
                        logger.warn("[WhatsAppUtility] Template paused by Meta for message {}", metaMessageId);
                    }
                }
                case RETRY_WITH_BACKOFF, MARK_FAILED_PERMANENT, PAUSE_DISPATCH_QUEUE -> {}
            }
        }
    }

    // =========================================================================
    // Parsing & Format Helpers
    // =========================================================================

    public static Set<String> parseAttributes(String attrString) {
        if (attrString == null || attrString.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(attrString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
