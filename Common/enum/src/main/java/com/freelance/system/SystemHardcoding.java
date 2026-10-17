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

    /**
     * Known UUID representing the anonymous unauthenticated caller.
     */
    ANONYMOUS_USER_ID(UUID.fromString("11111111-1111-1111-1111-111111111111")),

    /**
     * Known UUID representing vendor technical support actor.
     */
    VENDOR_SUPPORT_USER_ID(UUID.fromString("99999999-9999-9999-9999-999999999999")),

    /**
     * Well-known session key identifier for authenticated vendor technical support sessions.
     */
    VENDOR_SUPPORT_SESSION_KEY("VENDOR_SUPPORT_SESSION"),

    /**
     * Default fallback tenant identifier for single-tenant operations and default provisioning.
     */
    DEFAULT_TENANT("freelance"),

    /**
     * Well-known identifier for Meta WhatsApp provider.
     */
    META("META");

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