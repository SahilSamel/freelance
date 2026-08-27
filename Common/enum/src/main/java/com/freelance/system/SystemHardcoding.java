package com.freelance.system;

import java.util.UUID;

/**
 * Declares system-wide immutable constants and well-known identifiers.
 */
public enum SystemHardcoding {

    /**
     * Known UUID representing the automated system actor.
     */
    SYSTEM_USER_ID(UUID.fromString("00000000-0000-0000-0000-000000000000")),

    ANONYMOUS_USER_ID(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    private final Object value;

    SystemHardcoding(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public UUID asUuid() {
        return (UUID) value;
    }

    public String asString() {
        return String.valueOf(value);
    }
}