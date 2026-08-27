package com.freelance.rest.app.endpoint.annotation;

import com.freelance.authorization.SystemRoles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares security and permission constraints for JAX-RS endpoint methods.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Authorization {

    /**
     * Unique permission key identifying this endpoint operation.
     * <p>
     * <b>Important:</b> Changing or renaming an existing authorization key in code is strongly inadvisable.
     * The key serves as the unique natural identity in the database; modifying it will cause startup seeders
     * to treat it as a deletion of the old permission and an insertion of a new one, breaking existing custom
     * role-to-permission mappings in the database unless explicitly migrated via a database migration script.
     * </p>
     *
     * @return unique permission key string
     */
    String key();

    /**
     * Human-readable display name for this permission.
     *
     * @return permission display name
     */
    String name();

    /**
     * Hierarchical grouping path used for organization, categorization, and grouping in responses
     *
     * @return hierarchical group name
     */
    String group();

    /**
     * Detailed administrative description of the action permitted by this authorization key.
     *
     * @return permission description
     */
    String description();

    /**
     * Default system role assigned to this permission during initial database seeding.
     *
     * @return default {@link SystemRoles} assignment, defaulting to {@link SystemRoles#NONE}
     */
    SystemRoles role() default SystemRoles.NONE;
}
