package com.freelance.rest.jaxrs.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Global JAX-RS request filter establishing the active tenant identifier.
 * <p>
 * Configured with {@code @Priority(0)} to execute before all other filters (including Audit and Transaction filters),
 * resolving the tenant context from the {@code X-Tenant-ID} request header or host subdomain.
 * </p>
 */
@Provider
@Priority(0)
public class TenantFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TenantFilter.class);

    public static final String TENANT_HEADER = "X-Tenant-ID";
    public static final String TENANT_PROPERTY = "request.tenantId";
    public static final String DEFAULT_TENANT = com.freelance.system.SystemHardcoding.DEFAULT_TENANT.asString();

    private static final Pattern TENANT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{2,64}$");
    private static final Pattern IPV4_PATTERN = Pattern.compile("^([0-9]{1,3}\\.){3}[0-9]{1,3}(:[0-9]+)?$");

    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "coverage",
            "favicon.ico"
    );

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo() != null ? requestContext.getUriInfo().getPath() : "";
        if (isExcluded(path)) {
            requestContext.setProperty(TENANT_PROPERTY, DEFAULT_TENANT);
            logger.debug("[TenantFilter] Path '{}' is excluded. Using default tenant context '{}'", path, DEFAULT_TENANT);
            return;
        }

        String tenantId = resolveTenantId(requestContext);

        if (tenantId == null || tenantId.isBlank()) {
            tenantId = DEFAULT_TENANT;
        }

        if (!isValidTenantId(tenantId)) {
            logger.warn("[TenantFilter] Invalid tenant identifier format: '{}'", tenantId);
            throw new WebApplicationException("Invalid tenant identifier format: " + tenantId, 400);
        }

        tenantId = tenantId.trim().toLowerCase();
        requestContext.setProperty(TENANT_PROPERTY, tenantId);
        logger.debug("[TenantFilter] Resolved tenant context: '{}' for request path '/{}'", tenantId, path);
    }

    private String resolveTenantId(ContainerRequestContext requestContext) {
        String headerTenant = requestContext.getHeaderString(TENANT_HEADER);
        if (headerTenant != null && !headerTenant.isBlank()) {
            return headerTenant.trim();
        }

        String host = requestContext.getHeaderString("Host");
        if (host == null || host.isBlank()) {
            if (requestContext.getUriInfo() != null && requestContext.getUriInfo().getBaseUri() != null) {
                host = requestContext.getUriInfo().getBaseUri().getHost();
            }
        }

        if (host != null && !host.isBlank()) {
            String extracted = extractSubdomain(host);
            if (extracted != null && !extracted.isBlank()) {
                return extracted;
            }
        }

        return null;
    }

    private String extractSubdomain(String host) {
        String cleanHost = host.contains(":") ? host.substring(0, host.indexOf(":")) : host;

        if (IPV4_PATTERN.matcher(cleanHost).matches() || "localhost".equalsIgnoreCase(cleanHost)) {
            return null;
        }

        String[] parts = cleanHost.split("\\.");
        if (parts.length >= 3) {
            String sub = parts[0].trim();
            if (!sub.equalsIgnoreCase("www") && !sub.equalsIgnoreCase("api")) {
                return sub;
            }
        }

        return null;
    }

    private boolean isValidTenantId(String tenantId) {
        return tenantId != null && TENANT_ID_PATTERN.matcher(tenantId).matches();
    }

    private boolean isExcluded(String path) {
        if (path == null) return false;
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        for (String excluded : EXCLUDED_PATHS) {
            if (cleanPath.equalsIgnoreCase(excluded) || cleanPath.startsWith(excluded + "/")) {
                return true;
            }
        }
        return false;
    }
}
