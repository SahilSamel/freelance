package com.freelance.config;

public final class DatabaseConfigHolder {

    private static volatile DatabaseConfig instance;

    private DatabaseConfigHolder() {
    }

    public static void set(DatabaseConfig config) {
        instance = config;
    }

    public static DatabaseConfig get() {
        return instance;
    }
}
