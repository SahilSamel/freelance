package com.freelance.keys.identityManagement.user;

import com.freelance.config.IError;

/**
 * Enumerates client-facing constraint violation errors for the user subdomain in identity management.
 */
public enum UserConstraintErrors implements IError {

    USER_ALREADY_EXISTS(
            "FL_3101",
            "A user with this username already exists.",
            "Raised when a user with the same identity already exists",
            409
    ),
    USER_NOT_FOUND(
            "FL_3102",
            "The requested user was not found.",
            "Raised when a user could not be found",
            404
    ),
    CANNOT_DELETE_SELF(
            "FL_3103",
            "You cannot delete your own account.",
            "Raised when caller attempts to delete their own account",
            400
    ),
    USER_TO_UPDATE_NOT_FOUND(
            "FL_3104",
            "The user you are trying to update does not exist.",
            "Raised when the target user for an update operation does not exist",
            404
    ),
    USER_TO_DELETE_NOT_FOUND(
            "FL_3105",
            "The user you are trying to delete does not exist.",
            "Raised when the target user for a delete operation does not exist",
            404
    ),
    CONTACT_ALREADY_EXISTS(
            "FL_3106",
            "A user with this contact information already exists.",
            "Raised when contact value is already registered for this channel",
            409
    );

    private static final String CATEGORY = "identity.user.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    UserConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
