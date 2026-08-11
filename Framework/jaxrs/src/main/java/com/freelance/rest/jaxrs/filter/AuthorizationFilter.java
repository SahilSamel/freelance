package com.freelance.rest.jaxrs.filter;

import com.freelance.authorization.service.AuthorizationService;
import com.freelance.rest.app.endpoint.annotation.Anonymous;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import com.freelance.rest.app.transaction.Transaction;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global JAX-RS filter that intercepts all API requests to ensure the authenticated user
 * is authorized to access the endpoint.
 */
@Provider
@Priority(3)
public class AuthorizationFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationFilter.class);

    @Context
    private ResourceInfo resourceInfo;

    private final AuthorizationService authorizationService;

    public AuthorizationFilter() {
        this.authorizationService = new AuthorizationService();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (isResourceAnonymous()) {
            return;
        }

        Authorization authAnnotation = getAuthorizationAnnotation();
        Transaction.getRequestContext().withAuthorizationKey(authAnnotation == null ? null : authAnnotation.key());
        authorizationService.isAuthorized();
    }

    private boolean isResourceAnonymous() {
        if (resourceInfo == null) return false;
        if (resourceInfo.getResourceMethod() != null && resourceInfo.getResourceMethod().isAnnotationPresent(Anonymous.class)) {
            return true;
        }
        if (resourceInfo.getResourceClass() != null && resourceInfo.getResourceClass().isAnnotationPresent(Anonymous.class)) {
            return true;
        }
        return false;
    }

    private Authorization getAuthorizationAnnotation() {
        if (resourceInfo == null) return null;
        
        if (resourceInfo.getResourceMethod() != null && resourceInfo.getResourceMethod().isAnnotationPresent(Authorization.class)) {
            return resourceInfo.getResourceMethod().getAnnotation(Authorization.class);
        }
        if (resourceInfo.getResourceClass() != null && resourceInfo.getResourceClass().isAnnotationPresent(Authorization.class)) {
            return resourceInfo.getResourceClass().getAnnotation(Authorization.class);
        }
        return null;
    }
}
