package com.freelance.consumer.alerts.outbound.whatsapp;

import com.freelance.alerts.LogicalAlertAction;

import java.util.Collections;
import java.util.Set;

/**
 * Standardized taxonomy and central registry for Meta WhatsApp Cloud API and internal WhatsApp alert error codes.
 * <p>
 * Unifies all Meta error codes, subcodes, normalized string representations,
 * human-readable descriptions, required {@link LogicalAlertAction}s, and retry policies.
 * </p>
 */
public enum WhatsAppErrorCode {

    // ── 1. RECIPIENT & USER ERRORS ──────────────────────────────────────────

    /**
     * Recipient phone number is not registered on WhatsApp or is in an unsupported format.
     * <p>
     * Meta Codes:
     * <ul>
     *   <li>{@code 131026} - Message undeliverable to recipient.</li>
     *   <li>{@code 131051} - Unsupported phone number.</li>
     *   <li>{@code 131052} - Phone number not associated with a WhatsApp account.</li>
     * </ul>
     * </p>
     */
    USER_NOT_ON_CHANNEL(
            "ERR_USER_NOT_ON_CHANNEL",
            "Recipient phone number does not exist on WhatsApp or format is unsupported.",
            LogicalAlertAction.DISABLE_USER_CHANNEL,
            false,
            Set.of(131026, 131051, 131052)
    ),

    /**
     * User has actively opted out of business notifications or blocked marketing/utility messages.
     * <p>
     * Meta Code:
     * <ul>
     *   <li>{@code 131026} with subcode {@code 221004} - User opted out of marketing/utility.</li>
     * </ul>
     * </p>
     */
    USER_OPTED_OUT(
            "ERR_USER_OPTED_OUT",
            "User has opted out or blocked business messaging on WhatsApp.",
            LogicalAlertAction.MARK_USER_OPTED_OUT,
            false,
            Collections.emptySet() // Identified via metaErrorSubcode == 221004
    ),

    /**
     * User has explicitly blocked the WhatsApp Business Account.
     * <p>
     * Meta Code:
     * <ul>
     *   <li>{@code 131049} - Meta delivered response indicating user has blocked the business.</li>
     * </ul>
     * </p>
     */
    USER_BLOCKED_BUSINESS(
            "ERR_USER_BLOCKED_BUSINESS",
            "User blocked the business on WhatsApp.",
            LogicalAlertAction.MARK_USER_OPTED_OUT,
            false,
            Set.of(131049)
    ),

    // ── 2. POLICY & CONVERSATION WINDOW ERRORS ──────────────────────────────

    /**
     * More than 24 hours have elapsed since the user's last inbound message.
     * Free-form session messages are forbidden by Meta policy outside this window; a pre-approved template is required.
     * <p>
     * Meta Codes:
     * <ul>
     *   <li>{@code 131047} - Re-engagement message: more than 24 hours have elapsed.</li>
     *   <li>{@code 131045} - Message sent outside the 24-hour customer service window.</li>
     * </ul>
     * </p>
     */
    WINDOW_24H_EXPIRED(
            "ERR_24H_WINDOW_EXPIRED",
            "More than 24 hours have elapsed since user last engaged. Pre-approved template required.",
            LogicalAlertAction.TRIGGER_HUMAN_INTERVENTION,
            false,
            Set.of(131047, 131045)
    ),

    /**
     * Outbound alert was delayed waiting for a human agent/user reply on WhatsApp,
     * but all 24-hour scheduled retries were exhausted without resolution.
     */
    HUMAN_REPLY_TIMEOUT(
            "ERR_HUMAN_REPLY_TIMEOUT",
            "Outbound alert timed out waiting for human reply.",
            LogicalAlertAction.MARK_FAILED_PERMANENT,
            false,
            Collections.emptySet()
    ),

    // ── 3. ACCOUNT, AUTHENTICATION & BILLING ERRORS ─────────────────────────

    /**
     * WhatsApp Business Account (WABA) or phone number is banned, deregistered, or locked by Meta.
     * <p>
     * Meta Codes:
     * <ul>
     *   <li>{@code 131031} - Account locked or restricted.</li>
     *   <li>{@code 368} - Temporarily blocked for WhatsApp policy violations.</li>
     *   <li>{@code 133010} - Phone number deregistered from WhatsApp Business platform.</li>
     *   <li>{@code 133004} - Business account banned.</li>
     * </ul>
     * </p>
     */
    BUSINESS_ACCOUNT_LOCKED(
            "ERR_BUSINESS_ACCOUNT_LOCKED",
            "WhatsApp Business Account is locked, banned, or unregistered.",
            LogicalAlertAction.CRITICAL_ADMIN_ALERT,
            false,
            Set.of(131031, 368, 133010, 133004)
    ),

    /**
     * Meta System User OAuth access token is expired, revoked, or invalid.
     * <p>
     * Meta Codes:
     * <ul>
     *   <li>{@code 190} - Access token has expired or been invalidated.</li>
     *   <li>{@code 100} - Invalid parameter / invalid access token credentials.</li>
     * </ul>
     * </p>
     */
    META_AUTH_EXPIRED(
            "ERR_META_AUTH_EXPIRED",
            "Meta System User OAuth access token is expired or revoked.",
            LogicalAlertAction.CRITICAL_ADMIN_ALERT,
            false,
            Set.of(190, 100)
    ),

    /**
     * Business account payment method failed, billing threshold reached, or credit line exhausted.
     * <p>
     * Meta Codes:
     * <ul>
     *   <li>{@code 131042} - Business account payment issue or monthly credit line exhausted.</li>
     *   <li>{@code 131056} - Payment required to send messages.</li>
     * </ul>
     * </p>
     */
    META_PAYMENT_FAILED(
            "ERR_META_PAYMENT_FAILED",
            "Business account payment issue or monthly credit line exhausted.",
            LogicalAlertAction.CRITICAL_ADMIN_ALERT,
            false,
            Set.of(131042, 131056)
    ),

    // ── 4. TEMPLATE LIFECYCLE ERRORS ────────────────────────────────────────

    /**
     * Template is paused by Meta due to low user quality feedback, or variable parameter count mismatch.
     * <p>
     * Meta Codes:
     * <ul>
     *   <li>{@code 132001} - Template does not exist in the specified language or namespace.</li>
     *   <li>{@code 132015} - Template paused by Meta due to low quality rating.</li>
     *   <li>{@code 132000} - Number of parameters does not match expected template placeholders.</li>
     * </ul>
     * </p>
     */
    TEMPLATE_PAUSED(
            "ERR_TEMPLATE_PAUSED",
            "Template paused by Meta due to low quality rating or parameter count mismatch.",
            LogicalAlertAction.PAUSE_TEMPLATE,
            false,
            Set.of(132001, 132015, 132000)
    ),

    // ── 5. INFRASTRUCTURE, RATE LIMITING & DISPATCH ─────────────────────────

    /**
     * Rate limit reached on Meta WhatsApp Cloud API or Meta server 5xx temporary glitch.
     * <p>
     * Meta Codes:
     * <ul>
     *   <li>{@code 130429} - Cloud API rate limit reached.</li>
     *   <li>{@code 80007} - Throughput rate limit exceeded.</li>
     *   <li>{@code 131053} - Spam rate limit triggered.</li>
     *   <li>{@code 500} - Meta internal server error.</li>
     *   <li>{@code 503} - Meta service temporarily unavailable.</li>
     * </ul>
     * </p>
     */
    META_RATE_LIMITED(
            "ERR_META_RATE_LIMITED",
            "Meta WhatsApp Cloud API rate limit reached or server busy. Retrying with backoff.",
            LogicalAlertAction.RETRY_WITH_BACKOFF,
            true,
            Set.of(130429, 80007, 131053, 500, 503)
    ),

    /**
     * HTTP dispatch call to Meta Cloud API endpoint failed (network error, timeout, non-2xx response).
     */
    DISPATCH_FAILED(
            "ERR_DISPATCH_FAILED",
            "Meta WhatsApp Cloud API HTTP dispatch failed.",
            LogicalAlertAction.MARK_FAILED_PERMANENT,
            false,
            Collections.emptySet()
    ),

    /**
     * Unclassified Meta WhatsApp error code.
     */
    UNKNOWN(
            "ERR_META_UNKNOWN",
            "Unclassified Meta WhatsApp error.",
            LogicalAlertAction.MARK_FAILED_PERMANENT,
            false,
            Collections.emptySet()
    );

    // ── Public Constants for Direct String References ───────────────────────
    public static final String CODE_USER_NOT_ON_CHANNEL = "ERR_USER_NOT_ON_CHANNEL";
    public static final String CODE_USER_OPTED_OUT = "ERR_USER_OPTED_OUT";
    public static final String CODE_USER_BLOCKED_BUSINESS = "ERR_USER_BLOCKED_BUSINESS";
    public static final String CODE_24H_WINDOW_EXPIRED = "ERR_24H_WINDOW_EXPIRED";
    public static final String CODE_HUMAN_REPLY_TIMEOUT = "ERR_HUMAN_REPLY_TIMEOUT";
    public static final String CODE_BUSINESS_ACCOUNT_LOCKED = "ERR_BUSINESS_ACCOUNT_LOCKED";
    public static final String CODE_META_AUTH_EXPIRED = "ERR_META_AUTH_EXPIRED";
    public static final String CODE_META_PAYMENT_FAILED = "ERR_META_PAYMENT_FAILED";
    public static final String CODE_TEMPLATE_PAUSED = "ERR_TEMPLATE_PAUSED";
    public static final String CODE_META_RATE_LIMITED = "ERR_META_RATE_LIMITED";
    public static final String CODE_DISPATCH_FAILED = "ERR_DISPATCH_FAILED";

    private final String code;
    private final String description;
    private final LogicalAlertAction action;
    private final boolean retriable;
    private final Set<Integer> metaErrorCodes;

    WhatsAppErrorCode(
            String code,
            String description,
            LogicalAlertAction action,
            boolean retriable,
            Set<Integer> metaErrorCodes
    ) {
        this.code = code;
        this.description = description;
        this.action = action;
        this.retriable = retriable;
        this.metaErrorCodes = metaErrorCodes;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public LogicalAlertAction getAction() {
        return action;
    }

    public boolean isRetriable() {
        return retriable;
    }

    public Set<Integer> getMetaErrorCodes() {
        return metaErrorCodes;
    }

    /**
     * Resolves a raw Meta error code and optional subcode to the corresponding {@link WhatsAppErrorCode}.
     *
     * @param metaErrorCode    primary error code returned by Meta WhatsApp Cloud API
     * @param metaErrorSubcode optional error subcode (e.g. 221004 for user opt-out)
     * @return the matched {@link WhatsAppErrorCode}, or {@link #UNKNOWN} if unmapped
     */
    public static WhatsAppErrorCode fromMeta(int metaErrorCode, Integer metaErrorSubcode) {
        if ((metaErrorCode == 131026 || metaErrorCode == 131051 || metaErrorCode == 131052)
                && metaErrorSubcode != null && metaErrorSubcode == 221004) {
            return USER_OPTED_OUT;
        }

        for (WhatsAppErrorCode candidate : values()) {
            if (candidate.metaErrorCodes.contains(metaErrorCode)) {
                return candidate;
            }
        }

        return UNKNOWN;
    }
}
