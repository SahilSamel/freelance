package com.freelance.user.create.validation;

import com.freelance.ConfigurationEngine;
import com.freelance.alerts.AlertChannel;
import com.freelance.keys.identityManagement.user.UserConfigKeys;
import com.freelance.keys.identityManagement.user.UserValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;
import com.freelance.user.contact.UserContactRequestDTO;
import com.freelance.user.create.UserCreateRequestDTO;

import java.util.HashSet;
import java.util.Set;

public class UserCreateValidation extends AbstractValidation {

    public void validateRequest(UserCreateRequestDTO request) {
        if (request == null) {
            raiseValidationError(UserValidationErrors.INVALID_USERNAME, "username");
            raiseValidationError(UserValidationErrors.INVALID_PASSWORD, "password");
            raiseValidationError(UserValidationErrors.CONTACT_INFO_REQUIRED, "contacts");
            validate();
            return;
        }

        int minUsernameLength = ConfigurationEngine.getInt(UserConfigKeys.USERNAME_MINIMUM_LENGTH);
        int maxUsernameLength = ConfigurationEngine.getInt(UserConfigKeys.USERNAME_MAXIMUM_LENGTH);
        int minPasswordLength = ConfigurationEngine.getInt(UserConfigKeys.PASSWORD_MINIMUM_LENGTH);

        String usernamePattern = ConfigurationEngine.get(UserConfigKeys.USERNAME_PATTERN);
        if (request.username() == null || request.username().isBlank()) {
            raiseValidationError(UserValidationErrors.INVALID_USERNAME, "username");
        } else {
            if (request.username().length() < minUsernameLength) {
                raiseValidationError(UserValidationErrors.USERNAME_TOO_SHORT, "username");
            }
            if (request.username().length() > maxUsernameLength) {
                raiseValidationError(UserValidationErrors.USERNAME_TOO_LONG, "username");
            }
            if (usernamePattern != null && !request.username().matches(usernamePattern)) {
                raiseValidationError(UserValidationErrors.USERNAME_HAS_INVALID_CHARS, "username");
            }
        }

        if (request.password() == null || request.password().isBlank()) {
            raiseValidationError(UserValidationErrors.INVALID_PASSWORD, "password");
        } else if (request.password().length() < minPasswordLength) {
            raiseValidationError(UserValidationErrors.PASSWORD_TOO_SHORT, "password");
        }

        if (request.contacts() == null || request.contacts().isEmpty()) {
            raiseValidationError(UserValidationErrors.CONTACT_INFO_REQUIRED, "contacts");
        } else {
            Set<AlertChannel> seenChannels = new HashSet<>();
            Set<String> seenValues = new HashSet<>();
            for (UserContactRequestDTO contact : request.contacts()) {
                if (contact == null) {
                    raiseValidationError(UserValidationErrors.CONTACT_INFO_REQUIRED, "contacts");
                    continue;
                }
                if (contact.channel() == null) {
                    raiseValidationError(UserValidationErrors.INVALID_CONTACT_CHANNEL, "contacts.channel");
                }
                if (contact.contactValue() == null || contact.contactValue().isBlank()) {
                    raiseValidationError(UserValidationErrors.INVALID_CONTACT_VALUE, "contacts.contactValue");
                } else {
                    String trimmedValue = contact.contactValue().trim().toLowerCase();
                    if (!seenValues.add(trimmedValue)) {
                        raiseValidationError(UserValidationErrors.DUPLICATE_CONTACT_VALUE, "contacts.contactValue");
                    }
                }
                if (contact.channel() != null && !seenChannels.add(contact.channel())) {
                    raiseValidationError(UserValidationErrors.DUPLICATE_CONTACT_CHANNEL, "contacts.channel");
                }
            }
        }

        validate();
    }
}
