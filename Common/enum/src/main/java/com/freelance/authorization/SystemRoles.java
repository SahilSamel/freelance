package com.freelance.authorization;

/**
 * Enumerates recognized system-level roles within the authorization engine.
 */
public enum SystemRoles {

    /**
     * Represents the absence of an explicit system role assignment.
     */
    NONE,

    /**
     * Represents the super administrator role holding global administrative privileges.
     */
    SUPER_ADMIN
}