package com.freelance.config;

/**
 * Defines the contract for system configuration properties and their code-level defaults.
 * <p>
 * <b>Package Requirement:</b> All enum implementations of this interface <b>MUST</b> be located
 * within the package {@code com.freelance.keys} (or its subpackages) across any module.
 * {@code ConfigurationRegistry} scans this specific package at runtime to discover, validate,
 * and seed all configuration definitions into the database. Implementations placed in any other package will not be discovered.
 * </p>
 */
public interface IConfig {

    /**
     * Retrieves the unique dot-separated hierarchical key identifying this property.
     * <p>
     * <b>Important:</b> Changing or renaming an existing key in code is strongly inadvisable.
     * The key serves as the unique natural identity in the database; modifying it will cause startup seeders
     * to treat it as a deletion of the old key and an insertion of a new one, resetting any custom administrator
     * overrides to code defaults unless explicitly migrated via a database migration script.
     * </p>
     * <p>
     * <b>Key Naming Convention:</b> {@code "<module>.<category>.<propertyName>"} (e.g., {@code "domain.subdomain.config_constant"}).
     * <ul>
     *   <li>Leading segments establish the hierarchical category path used to organize the properties in responses.</li>
     *   <li>The terminal segment specifies the distinct property name displayed as the field label.</li>
     * </ul>
     * </p>
     *
     * @return unique dot-separated configuration key string
     */
    String getKey();

    /**
     * Retrieves the default fallback value for this configuration property.
     *
     * @return default configuration value string
     */
    String getDefaultValue();

    /**
     * Retrieves a human-readable description explaining this configuration property's purpose.
     *
     * @return human-readable description string
     */
    String getDescription();
}
