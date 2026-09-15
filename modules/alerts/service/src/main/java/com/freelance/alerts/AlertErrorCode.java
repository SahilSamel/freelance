package com.freelance.alerts;

/**
 * Standardized failure error codes for the alerts and notifications subsystem.
 * <p>
 * Centralizes all error codes stored in {@code FL_ALERT_MESSAGE.error_code} when an alert
 * fails pre-dispatch validation, channel dispatch, or retry lifecycle execution.
 * </p>
 */
public enum AlertErrorCode {

    // ── 1. PRE-DISPATCH VALIDATION & CONSUMER PRECONDITIONS ─────────────────

    /**
     * The alert template's configured channel does not match the channel of the receiving consumer.
     */
    CHANNEL_MISMATCH(
            "ERR_CHANNEL_MISMATCH",
            "Template channel %s does not match consumer channel %s",
            "Template channel does not match consumer channel"
    ),

    /**
     * The recipient user ID does not exist in the UserRepository.
     */
    INVALID_USER_ID(
            "ERR_INVALID_USER_ID",
            "User %s does not exist in the system",
            "User does not exist in the system"
    ),

    /**
     * No dispatcher implementation is registered in AlertDispatcherFactory for the requested channel.
     */
    CHANNEL_NOT_AVAILABLE(
            "ERR_CHANNEL_NOT_AVAILABLE",
            "No dispatcher available for channel: %s",
            "No dispatcher available for the requested channel"
    ),

    /**
     * User has opted out of notifications on this channel.
     */
    USER_OPTED_OUT(
            "ERR_USER_OPTED_OUT",
            "User %s has opted out of %s messages (is_opted_out = true)",
            "User opted out of notifications on this channel"
    ),

    /**
     * Recipient contact is marked inactive.
     */
    CHANNEL_INACTIVE(
            "ERR_CHANNEL_INACTIVE",
            "Contact for user %s on channel %s is marked inactive (is_active = false)",
            "Recipient contact is marked inactive"
    ),

    /**
     * Global channel is currently paused or down.
     */
    CHANNEL_PAUSED(
            "ERR_CHANNEL_PAUSED",
            "Channel %s is marked inactive or paused: %s",
            "Channel is currently paused or inactive"
    ),

    /**
     * The requested template is not in APPROVED status.
     */
    TEMPLATE_NOT_APPROVED(
            "ERR_TEMPLATE_NOT_APPROVED",
            "Template '%s' is not approved for dispatch (current status: %s)",
            "Template is not in APPROVED state"
    ),

    /**
     * No contact information (phone number or email) was found for the user on this channel.
     */
    NO_CONTACT_FOUND(
            "ERR_NO_CONTACT_FOUND",
            "No contact found for user on channel %s",
            "No contact found for user on the requested channel"
    ),

    /**
     * The recipient contact address/phone number is blank, malformed, or rejected by the provider.
     */
    INVALID_RECIPIENT(
            "ERR_INVALID_RECIPIENT",
            "Recipient contact value is missing or invalid: %s",
            "Recipient contact value is missing or invalid"
    ),

    /**
     * An interactive list alert requires an active 24-hour customer session window, but no active session was found.
     */
    SESSION_WINDOW_CLOSED(
            "ERR_SESSION_WINDOW_CLOSED",
            "Interactive list template '%s' requires an active 24-hour customer session",
            "Interactive list requires an active 24-hour customer session"
    ),

    // ── 2. DISPATCH & DOWNSTREAM PROVIDER FAILURES ──────────────────────────

    /**
     * The outbound dispatch call to the downstream channel provider (Meta Cloud API) failed.
     */
    DISPATCH_FAILED(
            "ERR_DISPATCH_FAILED",
            "Downstream channel provider dispatch call failed: %s",
            "Downstream channel provider dispatch call failed"
    ),

    // ── 3. LIFECYCLE, DELAY & RETRY FAILURES ────────────────────────────────

    /**
     * The alert was delayed waiting for a human reply, but all scheduled 24h retry attempts were exhausted.
     */
    HUMAN_REPLY_TIMEOUT(
            "ERR_HUMAN_REPLY_TIMEOUT",
            "Outbound alert timed out waiting for human reply",
            "Outbound alert timed out waiting for human reply"
    ),

    /**
     * The event processing encountered fatal or repeated failures and exhausted all configured retry attempts.
     */
    MAX_RETRIES_EXCEEDED(
            "ERR_MAX_RETRIES_EXCEEDED",
            "Exhausted all configured retry attempts: %s",
            "Exhausted all configured retry attempts"
    ),

    // ── 4. META WHATSAPP SPECIFIC DELIVERY FAILURES ─────────────────────────

    /**
     * The recipient phone number is not registered on WhatsApp.
     */
    USER_NOT_ON_CHANNEL(
            "ERR_USER_NOT_ON_CHANNEL",
            "Recipient phone number does not exist on WhatsApp"
    ),

    /**
     * The recipient has explicitly blocked the WhatsApp Business Account.
     */
    USER_BLOCKED_BUSINESS(
            "ERR_USER_BLOCKED_BUSINESS",
            "User blocked the business on WhatsApp"
    ),

    /**
     * More than 24 hours have elapsed since the user's last inbound message. Template required.
     */
    WINDOW_24H_EXPIRED(
            "ERR_24H_WINDOW_EXPIRED",
            "More than 24 hours have elapsed since user last engaged; template required"
    ),

    /**
     * The WhatsApp Business Account is locked, banned, or unregistered.
     */
    BUSINESS_ACCOUNT_LOCKED(
            "ERR_BUSINESS_ACCOUNT_LOCKED",
            "WhatsApp Business Account is locked, banned, or unregistered"
    ),

    /**
     * The Meta System User OAuth access token is expired or revoked.
     */
    META_AUTH_EXPIRED(
            "ERR_META_AUTH_EXPIRED",
            "Meta System User OAuth access token is expired or revoked"
    ),

    /**
     * Meta business account payment issue or monthly credit line exhausted.
     */
    META_PAYMENT_FAILED(
            "ERR_META_PAYMENT_FAILED",
            "Business account payment issue or monthly credit line exhausted"
    ),

    /**
     * The message template is paused by Meta due to low quality rating or parameter mismatch.
     */
    TEMPLATE_PAUSED(
            "ERR_TEMPLATE_PAUSED",
            "Template paused by Meta due to low quality rating or parameter count mismatch"
    ),

    /**
     * Meta WhatsApp Cloud API rate limit reached or server is temporarily busy.
     */
    META_RATE_LIMITED(
            "ERR_META_RATE_LIMITED",
            "Meta WhatsApp Cloud API rate limit reached or server busy"
    ),

    /**
     * Unclassified Meta WhatsApp error.
     */
    META_UNKNOWN(
            "ERR_META_UNKNOWN",
            "Unclassified Meta WhatsApp error"
    );

    // ── Public String Constants for Direct String References ────────────────
    public static final String ERR_CHANNEL_MISMATCH = "ERR_CHANNEL_MISMATCH";
    public static final String ERR_INVALID_USER_ID = "ERR_INVALID_USER_ID";
    public static final String ERR_CHANNEL_NOT_AVAILABLE = "ERR_CHANNEL_NOT_AVAILABLE";
    public static final String ERR_CHANNEL_PAUSED = "ERR_CHANNEL_PAUSED";
    public static final String ERR_CHANNEL_INACTIVE = "ERR_CHANNEL_INACTIVE";
    public static final String ERR_TEMPLATE_NOT_APPROVED = "ERR_TEMPLATE_NOT_APPROVED";
    public static final String ERR_USER_OPTED_OUT = "ERR_USER_OPTED_OUT";
    public static final String ERR_NO_CONTACT_FOUND = "ERR_NO_CONTACT_FOUND";
    public static final String ERR_INVALID_RECIPIENT = "ERR_INVALID_RECIPIENT";
    public static final String ERR_SESSION_WINDOW_CLOSED = "ERR_SESSION_WINDOW_CLOSED";
    public static final String ERR_DISPATCH_FAILED = "ERR_DISPATCH_FAILED";
    public static final String ERR_HUMAN_REPLY_TIMEOUT = "ERR_HUMAN_REPLY_TIMEOUT";
    public static final String ERR_MAX_RETRIES_EXCEEDED = "ERR_MAX_RETRIES_EXCEEDED";
    public static final String ERR_USER_NOT_ON_CHANNEL = "ERR_USER_NOT_ON_CHANNEL";
    public static final String ERR_USER_BLOCKED_BUSINESS = "ERR_USER_BLOCKED_BUSINESS";
    public static final String ERR_24H_WINDOW_EXPIRED = "ERR_24H_WINDOW_EXPIRED";
    public static final String ERR_BUSINESS_ACCOUNT_LOCKED = "ERR_BUSINESS_ACCOUNT_LOCKED";
    public static final String ERR_META_AUTH_EXPIRED = "ERR_META_AUTH_EXPIRED";
    public static final String ERR_META_PAYMENT_FAILED = "ERR_META_PAYMENT_FAILED";
    public static final String ERR_TEMPLATE_PAUSED = "ERR_TEMPLATE_PAUSED";
    public static final String ERR_META_RATE_LIMITED = "ERR_META_RATE_LIMITED";
    public static final String ERR_META_UNKNOWN = "ERR_META_UNKNOWN";

    private final String code;
    private final String formatTemplate;
    private final String description;

    AlertErrorCode(String code, String description) {
        this(code, description, description);
    }

    AlertErrorCode(String code, String formatTemplate, String description) {
        this.code = code;
        this.formatTemplate = formatTemplate;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getFormatTemplate() {
        return formatTemplate;
    }

    /**
     * Formats the failure reason with provided context parameters.
     * If no parameters are provided or formatting fails, returns the default description.
     */
    public String format(Object... args) {
        if (args == null || args.length == 0) {
            return description;
        }
        try {
            return String.format(formatTemplate, args);
        } catch (Exception e) {
            return description;
        }
    }

    public static AlertErrorCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (AlertErrorCode candidate : values()) {
            if (candidate.code.equalsIgnoreCase(code.trim())) {
                return candidate;
            }
        }
        return null;
    }
}
