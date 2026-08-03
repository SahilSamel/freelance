package com.freelance.identityManagement;

public enum IdentityManagementKeys {
    USERNAME_MINIMUM_LENGTH("identityManagement.username.minimumLength"),
    USERNAME_MAXIMUM_LENGTH("identityManagement.username.maximumLength");

    private final String key;

    IdentityManagementKeys(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
