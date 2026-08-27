package com.freelance.config;

/**
 * Defines the contract for application errors, client-facing responses, and their code-level defaults.
 * <p>
 * <b>Package Requirement:</b> All enum implementations of this interface <b>MUST</b> be located
 * within the package {@code com.freelance.keys} (or its subpackages) across any module.
 * {@code ConfigurationRegistry} scans this specific package at runtime to discover, validate,
 * and seed all error definitions into the database. Implementations placed in any other package will not be discovered.
 * </p>
 */
public interface IError {

    /**
     * Retrieves the unique dot-separated hierarchical error key.
     * <p>
     * <b>Important:</b> Changing or renaming an existing error key in code is strongly inadvisable.
     * The key serves as the unique natural identity in the database; modifying it will cause startup seeders
     * to treat it as a deletion of the old key and an insertion of a new one, resetting any custom administrator
     * overrides to code defaults unless explicitly migrated via a database migration script.
     * </p>
     * <p>
     * <b>Key Naming Convention:</b> {@code "<module>.<category>.<ERROR_CONSTANT>"} (e.g., {@code "domain.subdomain.error_constant"}).
     * <ul>
     *   <li>Leading segments establish the hierarchical category path used to organize and display errors in responses.</li>
     *   <li>The terminal segment specifies the distinct error constant name.</li>
     * </ul>
     * </p>
     *
     * @return unique dot-separated error key string
     */
    String getErrorKey();

    /**
     * Retrieves the unique, opaque indirection error code returned in client API responses.
     * <p>
     * <b>Format:</b> Prefixed alphanumeric identifier (e.g., {@code "FL_1001"}) that obscures internal error names
     * while providing client applications and log tracing with a stable reference code. Must be globally unique across all errors.
     * </p>
     * <p>
     * <b>Module Range Allocations:</b>
     * <ul>
     *   <li>{@code FL_1000} – {@code FL_1999}: Framework &amp; Infrastructure</li>
     *   <li>{@code FL_2000} – {@code FL_2999}: Authentication</li>
     *   <li>{@code FL_3000} – {@code FL_3999}: Identity Management</li>
     *   <li>{@code FL_4000} – {@code FL_4999}: Authorization &amp; RBAC</li>
     *   <li>{@code FL_5000} – {@code FL_5999}: Audit Logging</li>
     * </ul>
     * </p>
     *
     * @return unique client-facing indirection code string
     */
    String getIndirectionCode();

    /**
     * Retrieves the canonical default HTTP status code associated with this error.
     *
     * @return default HTTP status code (e.g., 400, 401, 403, 404, 500)
     */
    int getDefaultStatusCode();

    /**
     * Retrieves the canonical default user-facing message associated with this error.
     *
     * @return default user-facing error message string
     */
    String getDefaultMessage();

    /**
     * Retrieves a human-readable description explaining the conditions under which this error is triggered.
     *
     * @return human-readable description string
     */
    String getDescription();
}