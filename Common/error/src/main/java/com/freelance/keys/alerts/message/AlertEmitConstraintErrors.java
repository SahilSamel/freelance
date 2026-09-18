package com.freelance.keys.alerts.message;

import com.freelance.config.IError;

/**
 * Enumerates client-facing constraint violation errors for dispatching / emitting alerts.
 */
public enum AlertEmitConstraintErrors implements IError {

    USER_CONTACT_NOT_FOUND(
            "FL_7104",
            "No contact details found for the target user on the selected channel.",
            "Recipient contact information missing in FL_USER_CONTACT",
            404
    ),
    USER_OPTED_OUT_OF_CHANNEL(
            "FL_7105",
            "The target user has opted out or the channel is inactive.",
            "User preference blocks delivery on this channel",
            400
    ),
    META_DISPATCH_FAILED(
            "FL_7107",
            "Failed to dispatch message to Meta WhatsApp Cloud API.",
            "Meta HTTP request failed with permanent error",
            502
    );

    private static final String CATEGORY = "alerts.message.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    AlertEmitConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
        this.indirectionCode = indirectionCode;
        this.defaultMessage = defaultMessage;
        this.description = description;
        this.defaultStatusCode = defaultStatusCode;
    }

    @Override
    public String getErrorKey() {
        return CATEGORY + "." + name();
    }

    @Override
    public String getIndirectionCode() {
        return this.indirectionCode;
    }

    @Override
    public String getDefaultMessage() {
        return this.defaultMessage;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public int getDefaultStatusCode() {
        return this.defaultStatusCode;
    }
}
