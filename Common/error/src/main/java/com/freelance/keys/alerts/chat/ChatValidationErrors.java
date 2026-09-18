package com.freelance.keys.alerts.chat;

import com.freelance.config.IError;

/**
 * Enumerates client-facing field validation errors for chat timeline and message replies.
 */
public enum ChatValidationErrors implements IError {

    INVALID_CHAT_REPLY(
            "FL_7006",
            "The chat reply message content cannot be blank.",
            "Chat reply validation failed",
            400
    );

    private static final String CATEGORY = "alerts.chat.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    ChatValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
