package com.freelance.rest.jaxrs.filter;

import com.freelance.auth.dto.AuthVerifyRequestDTO;
import com.freelance.auth.dto.AuthVerifyResponseDTO;
import com.freelance.auth.service.Auth;
import com.freelance.rest.app.endpoint.annotation.Anonymous;
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
 * Global JAX-RS filter that intercepts all API requests to ensure they carry
 * a valid JWT token, unless the target method or class is annotated with @Anonymous.
 */
@Provider
@Priority(2)
public class AuthFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Injected by Jersey; describes which resource method/class is about to handle this request.
     */
    @Context
    private ResourceInfo resourceInfo;

    /**
     * Authenticates the incoming request, unless the target endpoint opts
     * out via @Anonymous. On success, binds the authenticated user's ID to
     * the current transaction for downstream services to use.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        String requestPath = requestContext.getUriInfo() != null ? requestContext.getUriInfo().getPath() : "unknown";
        logger.debug("Intercepted request for: /{}", requestPath);

        if (isAnonymousAccessAllowed()) {
            logger.debug("Endpoint is marked @Anonymous. Skipping authentication.");
            return;
        }

        logger.debug("Endpoint requires authentication. Extracting token...");
        String token = extractBearerToken(requestContext);

        if (token == null) {
            logger.warn("Authorization header missing or invalid for protected endpoint: /{}", requestPath);
        }

        Auth authService = new Auth();
        AuthVerifyResponseDTO verifyResponse = authService.verify(new AuthVerifyRequestDTO(token));

        if (verifyResponse != null && verifyResponse.userId() != null) {
            logger.debug("Authentication successful. User ID: {}", verifyResponse.userId());
        } else {
            logger.warn("Authentication verification failed for /{}", requestPath);
        }

        Transaction.getRequestContext().withUserId(verifyResponse.userId());
        Transaction.getRequestContext().withSessionKey(verifyResponse.sessionKey());
    }

    /**
     * Checks whether the target resource method or class is annotated with
     *
     * @Anonymous, in which case this filter should skip authentication
     * entirely for this request.
     */
    private boolean isAnonymousAccessAllowed() {
        if (resourceInfo == null) {
            logger.debug("ResourceInfo is null. Cannot check for @Anonymous annotation.");
            return false;
        }
        if (resourceInfo.getResourceMethod() != null && resourceInfo.getResourceMethod().isAnnotationPresent(Anonymous.class)) {
            return true;
        }
        if (resourceInfo.getResourceClass() != null && resourceInfo.getResourceClass().isAnnotationPresent(Anonymous.class)) {
            return true;
        }
        return false;
    }

    /**
     * Extracts the bearer token from the Authorization header, or null if
     * the header is absent or not in "Bearer &lt;token&gt;" form. A null
     * token is passed straight through to verify() rather than handled
     * here — "missing token" is an auth failure, and Auth already knows
     * how to raise that consistently.
     */
    private String extractBearerToken(ContainerRequestContext requestContext) {
        String authHeader = requestContext.getHeaderString(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authHeader.substring(BEARER_PREFIX.length()).trim();
    }
}