package com.freelance.rest.app.endpoint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Signals that an annotated JAX-RS resource method requires an authenticated caller session
 * with a valid active token, but skips specific Role-Based Access Control (RBAC) authorization checks.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Authenticated {
}
