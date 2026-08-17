package com.freelance.system;

import java.util.UUID;

public enum SystemHardcoding {

    SYSTEM_USER_ID(UUID.fromString("00000000-0000-0000-0000-000000000000"));

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

    public int asInt() {
        return (Integer) value;
    }
}