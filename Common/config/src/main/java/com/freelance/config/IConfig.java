package com.freelance.config;

/**
 * Standard contract for system configuration keys and their canonical defaults.
 */
public interface IConfig {

    /**
     * Unique configuration key/identifier in the database (e.g. "authentication.session.ttlSeconds").
     */
    String getKey();

    /**
     * Default value used at startup seeding and as fallback when absent from the database.
     */
    String getDefaultValue();

    /**
     * Human-readable description of what this configuration parameter controls.
     */
    String getDescription();
}
