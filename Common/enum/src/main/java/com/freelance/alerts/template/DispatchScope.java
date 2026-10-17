package com.freelance.alerts.template;

/**
 * Declares the intended dispatch scope for an alert or notification template.
 */
public enum DispatchScope {
    /**
     * Template is designed specifically for 1-on-1 personalized interactions (e.g., OTPs, receipts, individual chats).
     */
    INDIVIDUAL,

    /**
     * Template is designed specifically for broadcast or marketing campaigns sent to multiple recipients.
     */
    BULK,

    /**
     * Versatile template that can be dispatched both individually and in bulk broadcasts.
     */
    BOTH,

    /**
     * Automatically triggered system alert template (e.g. welcome onboarding, renewal confirmation, OTP).
     * Excluded from manual dispatch and user-facing template selectors.
     */
    AUTO
}
