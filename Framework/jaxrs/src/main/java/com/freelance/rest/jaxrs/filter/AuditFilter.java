package com.freelance.rest.jaxrs.filter;

import com.freelance.audit.dto.AuditLogCreateRequestDTO;
import com.freelance.audit.service.AuditService;
import com.freelance.rest.app.endpoint.annotation.Anonymous;
import com.freelance.rest.app.transaction.Transaction;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.UUID;

/**
 * Global JAX-RS filter that audits incoming requests and outgoing responses.
 * Intercepts request metadata at ingress and persists the full audit record
 * to the database at egress. Runs with strict compliance: if auditing fails,
 * the active transaction fails.
 */
@Provider
@Priority(1)
public class AuditFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuditFilter.class);

    private static final String START_TIME_PROP = "audit.startTime";
    private static final String TIMESTAMP_PROP = "audit.timestamp";
    private static final String TRACE_ID_PROP = "audit.traceId";
    private static final String TENANT_ID_PROP = "audit.tenantId";
    private static final String IP_ADDRESS_PROP = "audit.ipAddress";
    private static final String USER_AGENT_PROP = "audit.userAgent";
    private static final String AUTH_MECHANISM_PROP = "audit.authMechanism";
    private static final String USER_ID_PROP = "audit.userId";

    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String TENANT_ID_HEADER = "X-Tenant-ID";

    @Context
    private ResourceInfo resourceInfo;

    private final AuditService auditService;

    public AuditFilter() {
        this.auditService = new AuditService();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        long startTime = System.currentTimeMillis();
        Instant timestamp = Instant.now();

        String traceId = resolveTraceId(requestContext);
        MDC.put(TRACE_ID_MDC_KEY, traceId);

        String tenantId = extractTenantId(requestContext);
        String ipAddress = extractClientIp(requestContext);
        String userAgent = requestContext.getHeaderString("User-Agent");
        String authMechanism = resolveAuthMechanism(requestContext);

        requestContext.setProperty(START_TIME_PROP, startTime);
        requestContext.setProperty(TIMESTAMP_PROP, timestamp);
        requestContext.setProperty(TRACE_ID_PROP, traceId);
        requestContext.setProperty(TENANT_ID_PROP, tenantId);
        requestContext.setProperty(IP_ADDRESS_PROP, ipAddress);
        requestContext.setProperty(USER_AGENT_PROP, userAgent);
        requestContext.setProperty(AUTH_MECHANISM_PROP, authMechanism);

        logger.debug("[Audit] Request started: {} {} (traceId={})",
                requestContext.getMethod(),
                requestContext.getUriInfo() != null ? requestContext.getUriInfo().getPath() : "unknown",
                traceId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String traceId = (String) requestContext.getProperty(TRACE_ID_PROP);
        if (traceId == null || traceId.isBlank()) {
            traceId = resolveTraceId(requestContext);
        }

        MDC.put(TRACE_ID_MDC_KEY, traceId);

        try {
            Long startTime = (Long) requestContext.getProperty(START_TIME_PROP);
            long durationMs = startTime != null ? Math.max(0, System.currentTimeMillis() - startTime) : 0;

            Instant timestamp = (Instant) requestContext.getProperty(TIMESTAMP_PROP);
            if (timestamp == null) {
                timestamp = Instant.now();
            }

            String tenantId = (String) requestContext.getProperty(TENANT_ID_PROP);
            if (tenantId == null) {
                tenantId = extractTenantId(requestContext);
            }

            String ipAddress = (String) requestContext.getProperty(IP_ADDRESS_PROP);
            if (ipAddress == null) {
                ipAddress = extractClientIp(requestContext);
            }

            String userAgent = (String) requestContext.getProperty(USER_AGENT_PROP);
            if (userAgent == null) {
                userAgent = requestContext.getHeaderString("User-Agent");
            }

            String authMechanism = (String) requestContext.getProperty(AUTH_MECHANISM_PROP);
            if (authMechanism == null) {
                authMechanism = resolveAuthMechanism(requestContext);
            }

            String httpMethod = requestContext.getMethod() != null ? requestContext.getMethod() : "UNKNOWN";
            String endpoint = requestContext.getUriInfo() != null ? "/" + requestContext.getUriInfo().getPath() : "/";
            int statusCode = responseContext.getStatus();

            UUID userId = (UUID) requestContext.getProperty(USER_ID_PROP);

            AuditLogCreateRequestDTO auditDto = new AuditLogCreateRequestDTO(
                    timestamp,
                    traceId,
                    userId,
                    tenantId,
                    httpMethod,
                    endpoint,
                    statusCode,
                    durationMs,
                    ipAddress,
                    userAgent,
                    authMechanism
            );

            try (Transaction tx = Transaction.start()) {
                auditService.createAuditLog(auditDto);
            }

            responseContext.getHeaders().putSingle(TRACE_ID_HEADER, traceId);

            logger.debug("[Audit] Request completed: {} {} -> status={} duration={}ms",
                    httpMethod, endpoint, statusCode, durationMs);

        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    private String resolveTraceId(ContainerRequestContext requestContext) {
        String traceId = requestContext.getHeaderString(TRACE_ID_HEADER);
        if (traceId != null && !traceId.isBlank()) {
            return traceId.trim();
        }

        return UUID.randomUUID().toString();
    }

    private String extractTenantId(ContainerRequestContext requestContext) {
        String tenantId = requestContext.getHeaderString(TENANT_ID_HEADER);
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId.trim();
        }
        return null;
    }

    private String extractClientIp(ContainerRequestContext requestContext) {
        String forwardedFor = requestContext.getHeaderString("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] ips = forwardedFor.split(",");
            if (ips.length > 0 && !ips[0].isBlank()) {
                return ips[0].trim();
            }
        }

        String realIp = requestContext.getHeaderString("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return "127.0.0.1";
    }

    private String resolveAuthMechanism(ContainerRequestContext requestContext) {
        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader != null && !authHeader.isBlank()) {
            if (authHeader.startsWith("Bearer ")) {
                return "bearer";
            }
            if (authHeader.startsWith("ApiKey ")) {
                return "api_key";
            }
            if (authHeader.startsWith("Basic ")) {
                return "basic";
            }
            return "custom";
        }

        String apiKeyHeader = requestContext.getHeaderString("X-API-Key");
        if (apiKeyHeader != null && !apiKeyHeader.isBlank()) {
            return "api_key";
        }

        if (isAnonymousEndpoint()) {
            return "anonymous";
        }

        return "none";
    }

    private boolean isAnonymousEndpoint() {
        if (resourceInfo == null) {
            return false;
        }
        if (resourceInfo.getResourceMethod() != null && resourceInfo.getResourceMethod().isAnnotationPresent(Anonymous.class)) {
            return true;
        }
        return resourceInfo.getResourceClass() != null && resourceInfo.getResourceClass().isAnnotationPresent(Anonymous.class);
    }
}
