package com.freelance.rest.app.endpoint.annotation;

import com.freelance.authorization.SystemRoles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Secures a JAX-RS endpoint with Role-Based Access Control.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Authorization {
    /**
     * Unique endpoint identifier (e.g., "user.create")
     */
    String key();

    /**
     * Human-readable name.
     */
    String name();

    /**
     * Dot-notated hierarchy for UI nesting (e.g., "admin.user.management")
     */
    String group();

    /**
     * Description of role's logical access scope
     */
    String description();

    /**
     * Default system role ID to map this endpoint to. 
     * Defaults to com.freelance.authorization.SystemRoles.NONE (no system role).
     */
    SystemRoles role() default SystemRoles.NONE;
}
