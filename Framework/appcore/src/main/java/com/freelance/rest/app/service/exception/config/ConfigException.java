package com.freelance.rest.app.service.exception.config;

import com.freelance.keys.FrameworkErrorConstants;
import com.freelance.rest.app.service.exception.BaseException;

public class ConfigException extends BaseException {
    public ConfigException(String message) {
        super(FrameworkErrorConstants.MISSING_CONFIGURATION_ERROR, message);
    }
}
