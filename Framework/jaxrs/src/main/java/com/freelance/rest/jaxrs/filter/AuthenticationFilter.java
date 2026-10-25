package com.freelance.rest.jaxrs.filter;

import com.freelance.auth.dto.AuthVerifyRequestDTO;
import com.freelance.auth.dto.AuthVerifyResponseDTO;
import com.freelance.auth.service.Authentication;
import com.freelance.rest.app.endpoint.annotation.Anonymous;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.system.SystemHardcoding;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Global JAX-RS request filter enforcing authentication and establishing caller identity.
 * <p>
 * Configured with {@code @Priority(3)} to serve two primary purposes:
 * <ul>
 *   <li><b>Security:</b> Enforces authentication by extracting and validating security tokens before requests reach domain logic.</li>
 *   <li><b>Downstream Awareness:</b> Identifies whether the targeted endpoint is anonymous or protected (endpoints marked with
 *       the {@link Anonymous} annotation are considered anonymous), populating the request context so downstream filters and services
 *       know whether the request is anonymous or who the authenticated caller is.</li>
 * </ul>
 * </p>
 */
@Provider
@Priority(3)
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final Authentication authenticationService;

    public AuthenticationFilter() {
        this.authenticationService = new Authentication();
    }

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String requestPath = requestContext.getUriInfo() != null ? requestContext.getUriInfo().getPath() : "unknown";
        logger.debug("Intercepted request for: /{}", requestPath);

        if (isResourceAnonymous()) {
            UUID systemUserId = SystemHardcoding.ANONYMOUS_USER_ID.asUuid();
            Transaction.getRequestContext().withUserId(systemUserId);
            requestContext.setProperty("audit.userId", systemUserId);
            logger.debug("Endpoint is marked @Anonymous. Skipping authentication.");
            return;
        }

        logger.debug("Endpoint requires authentication. Extracting token...");
        String token = extractBearerToken(requestContext);

        AuthVerifyResponseDTO verifyResponse = authenticationService.verify(new AuthVerifyRequestDTO(token));
        Transaction.getRequestContext().withUserId(verifyResponse.userId());
        Transaction.getRequestContext().withSessionKey(verifyResponse.sessionKey());
        requestContext.setProperty("audit.userId", verifyResponse.userId());
    }

    private boolean isResourceAnonymous() {
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

    private String extractBearerToken(ContainerRequestContext requestContext) {
        String authHeader = requestContext.getHeaderString(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }

        // Fallback for native browser EventSource (SSE) which cannot send custom request headers
        if (requestContext.getUriInfo() != null && requestContext.getUriInfo().getQueryParameters() != null) {
            String queryToken = requestContext.getUriInfo().getQueryParameters().getFirst("token");
            if (queryToken != null && !queryToken.isBlank()) {
                return queryToken.trim();
            }
        }

        return null;
    }
}