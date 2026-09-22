package com.freelance.keys.identityManagement.user;

import com.freelance.config.IError;

/**
 * Enumerates client-facing field validation errors for the user subdomain in identity management.
 */
public enum UserValidationErrors implements IError {

    INVALID_USER_ID(
            "FL_3001",
            "The provided user ID is invalid or missing.",
            "User ID validation failed",
            400
    ),
    INVALID_USERNAME(
            "FL_3002",
            "The username is required and cannot be blank.",
            "Username is null or blank",
            400
    ),
    USERNAME_TOO_SHORT(
            "FL_3003",
            "The username does not meet the minimum length requirement.",
            "Username does not meet the minimum length requirement",
            400
    ),
    USERNAME_TOO_LONG(
            "FL_3004",
            "The username exceeds the maximum allowed length.",
            "Username exceeds the maximum allowed length",
            400
    ),
    USERNAME_HAS_INVALID_CHARS(
            "FL_3005",
            "The username contains invalid characters.",
            "Username contains characters outside the allowed set",
            400
    ),
    INVALID_PASSWORD(
            "FL_3006",
            "The password is required and cannot be blank.",
            "Password is null or blank",
            400
    ),
    PASSWORD_TOO_SHORT(
            "FL_3007",
            "The password is shorter than the required minimum length.",
            "Password does not meet the minimum length requirement",
            400
    ),
    CONTACT_INFO_REQUIRED(
            "FL_3008",
            "At least one contact information entry is required.",
            "Contact info is missing or empty",
            400
    ),
    INVALID_CONTACT_CHANNEL(
            "FL_3009",
            "The contact channel is required and must be a valid channel.",
            "Contact channel is null or invalid",
            400
    ),
    INVALID_CONTACT_VALUE(
            "FL_3010",
            "The contact value is required and cannot be blank.",
            "Contact value is null or blank",
            400
    ),
    DUPLICATE_CONTACT_CHANNEL(
            "FL_3011",
            "Duplicate contact channels are not allowed for the same user.",
            "Multiple contacts specified with the same channel",
            400
    ),
    DUPLICATE_CONTACT_VALUE(
            "FL_3012",
            "Duplicate contact values are not allowed within the same request.",
            "Multiple contacts specified with the same value",
            400
    );

    private static final String CATEGORY = "identity.user.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    UserValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
