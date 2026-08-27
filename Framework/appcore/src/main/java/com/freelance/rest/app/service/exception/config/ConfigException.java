package com.freelance.rest.app.service.exception.config;

import com.freelance.keys.FrameworkErrorConstants;
import com.freelance.rest.app.service.exception.BaseException;

/**
 * Thrown when a required configuration property is missing,
 * unparseable, or invalid.
 */
public class ConfigException extends BaseException {

    public ConfigException(String message) {
        super(FrameworkErrorConstants.MISSING_CONFIGURATION_ERROR, message);
    }
}
