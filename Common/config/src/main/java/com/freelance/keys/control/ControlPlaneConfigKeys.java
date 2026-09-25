package com.freelance.keys.control;

import com.freelance.config.IConfig;

public enum ControlPlaneConfigKeys implements IConfig {

    PLATFORM_VENDOR_PUBLIC_KEY(
            "control.platform.vendor.publicKey",
            "",
            "Base64-encoded X.509 RSA public key used across all tenants for vendor support access"
    ),

    PLATFORM_VENDOR_PRIVATE_KEY(
            "control.platform.vendor.privateKey",
            "",
            "PEM-encoded PKCS#8 RSA private key used by the control plane to mint ephemeral vendor support tokens"
    );

    private final String key;
    private final String defaultValue;
    private final String description;
    private final String validation;

    ControlPlaneConfigKeys(String key, String defaultValue, String description) {
        this(key, defaultValue, description, null);
    }

    ControlPlaneConfigKeys(String key, String defaultValue, String description, String validation) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.description = description;
        this.validation = validation;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getValidation() {
        return validation;
    }
}
