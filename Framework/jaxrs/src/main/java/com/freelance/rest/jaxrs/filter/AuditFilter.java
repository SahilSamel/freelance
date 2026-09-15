package com.freelance.rest.jaxrs.filter;

import com.freelance.ConfigurationEngine;
import com.freelance.audit.dto.AuditLogCreateRequestDTO;
import com.freelance.audit.service.AuditService;
import com.freelance.keys.audit.AuditConfigKeys;
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Global audit filter intercepting all incoming HTTP requests and outgoing responses.
 * <p>
 * Because auditing is a system task, this filter is wrapped around all other filters using {@code @Priority(1)}.
 * It executes first on ingress (to initialize trace IDs and timing) and last on egress, where it builds its own
 * dedicated {@link Transaction} context to persist the audit record.
 * </p>
 */
@Provider
@Priority(1)
public class AuditFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuditFilter.class);

    private static final String START_TIME_PROP = "audit.startTime";
    private static final String TIMESTAMP_PROP = "audit.timestamp";
    private static final String TRACE_ID_PROP = "audit.traceId";
    private static final String IP_ADDRESS_PROP = "audit.ipAddress";
    private static final String USER_AGENT_PROP = "audit.userAgent";
    private static final String AUTH_MECHANISM_PROP = "audit.authMechanism";
    private static final String USER_ID_PROP = "audit.userId";
    private static final String REQUEST_DATA_PROP = "audit.requestData";

    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final int MAX_REQUEST_DATA_LENGTH = 10000;

    private static final Pattern JSON_SENSITIVE_STRING_PATTERN = Pattern.compile(
            "(?i)(\"(?:password|newpassword|confirmpassword|oldpassword|token|accesstoken|refreshtoken|clientsecret|secret|apikey|pin|cvv)\"\\s*:\\s*)\"(?:\\\\\"|[^\"])*\""
    );

    private static final Pattern JSON_SENSITIVE_PRIMITIVE_PATTERN = Pattern.compile(
            "(?i)(\"(?:password|pin|cvv)\"\\s*:\\s*)([0-9]+)"
    );

    private static final Pattern QUERY_SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(password|token|secret|apikey|accesstoken|refreshtoken)=([^&]*)"
    );

    @Context
    private ResourceInfo resourceInfo;

    private final AuditService auditService;

    public AuditFilter() {
        this.auditService = new AuditService();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String traceId = resolveTraceId(requestContext);
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        requestContext.setProperty(TRACE_ID_PROP, traceId);

        if (!ConfigurationEngine.getBoolean(AuditConfigKeys.AUDITING_ENABLED)) {
            return;
        }

        long startTime = System.currentTimeMillis();
        Instant timestamp = Instant.now();

        String ipAddress = extractClientIp(requestContext);
        String userAgent = requestContext.getHeaderString("User-Agent");
        String authMechanism = resolveAuthMechanism(requestContext);
        String requestData = extractRequestData(requestContext);

        requestContext.setProperty(START_TIME_PROP, startTime);
        requestContext.setProperty(TIMESTAMP_PROP, timestamp);
        requestContext.setProperty(IP_ADDRESS_PROP, ipAddress);
        requestContext.setProperty(USER_AGENT_PROP, userAgent);
        requestContext.setProperty(AUTH_MECHANISM_PROP, authMechanism);
        requestContext.setProperty(REQUEST_DATA_PROP, requestData);

        logger.debug("[Audit] Request started: {} {} (traceId={})",
                requestContext.getMethod(),
                requestContext.getUriInfo() != null ? requestContext.getUriInfo().getPath() : "unknown",
                traceId);
    }
    
    /**
     * Records request telemetry and persists the audit log entry upon response delivery.
     * <p>
     * Because this filter executes outside {@link TransactionFilter}, it opens its own dedicated transaction
     * to synchronously persist the audit log record. This is handled synchronously as the application's expected
     * request volume is low.
     * </p>
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String traceId = (String) requestContext.getProperty(TRACE_ID_PROP);
        if (traceId == null || traceId.isBlank()) {
            traceId = resolveTraceId(requestContext);
        }

        MDC.put(TRACE_ID_MDC_KEY, traceId);
        responseContext.getHeaders().putSingle(TRACE_ID_HEADER, traceId);

        if (!ConfigurationEngine.getBoolean(AuditConfigKeys.AUDITING_ENABLED)) {
            MDC.remove(TRACE_ID_MDC_KEY);
            return;
        }

        try {
            Long startTime = (Long) requestContext.getProperty(START_TIME_PROP);
            long durationMs = startTime != null ? Math.max(0, System.currentTimeMillis() - startTime) : 0;

            Instant timestamp = (Instant) requestContext.getProperty(TIMESTAMP_PROP);
            if (timestamp == null) {
                timestamp = Instant.now();
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

            String requestData = (String) requestContext.getProperty(REQUEST_DATA_PROP);

            String httpMethod = requestContext.getMethod() != null ? requestContext.getMethod() : "UNKNOWN";
            String endpoint = requestContext.getUriInfo() != null ? "/" + requestContext.getUriInfo().getPath() : "/";
            int statusCode = responseContext.getStatus();

            UUID userId = (UUID) requestContext.getProperty(USER_ID_PROP);

            AuditLogCreateRequestDTO auditDto = new AuditLogCreateRequestDTO(
                    timestamp,
                    traceId,
                    userId,
                    httpMethod,
                    endpoint,
                    requestData,
                    statusCode,
                    durationMs,
                    ipAddress,
                    userAgent,
                    authMechanism
            );

            try (Transaction tx = Transaction.start()) {
                auditService.createAuditLog(auditDto);
            }

            logger.debug("[Audit] Request completed: {} {} -> status={} duration={}ms",
                    httpMethod, endpoint, statusCode, durationMs);

        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    private String extractRequestData(ContainerRequestContext requestContext) {
        String data = null;

        if (requestContext.hasEntity()) {
            try {
                InputStream entityStream = requestContext.getEntityStream();
                if (entityStream != null) {
                    byte[] bytes = entityStream.readAllBytes();
                    requestContext.setEntityStream(new ByteArrayInputStream(bytes));
                    if (bytes.length > 0) {
                        data = new String(bytes, StandardCharsets.UTF_8).trim();
                    }
                }
            } catch (Exception e) {
                logger.warn("[Audit] Failed to read request entity stream for audit logging", e);
            }
        }

        if ((data == null || data.isBlank()) && requestContext.getUriInfo() != null && requestContext.getUriInfo().getRequestUri() != null) {
            String rawQuery = requestContext.getUriInfo().getRequestUri().getRawQuery();
            if (rawQuery != null && !rawQuery.isBlank()) {
                data = "query: " + rawQuery.trim();
            }
        }

        if (data == null || data.isBlank()) {
            return null;
        }

        String sanitized = sanitizeRequestData(data);
        if (sanitized.length() > MAX_REQUEST_DATA_LENGTH) {
            return sanitized.substring(0, MAX_REQUEST_DATA_LENGTH) + "... [TRUNCATED]";
        }
        return sanitized;
    }

    String sanitizeRequestData(String data) {
        if (data == null || data.isBlank()) {
            return data;
        }
        String sanitized = JSON_SENSITIVE_STRING_PATTERN.matcher(data).replaceAll("$1\"******\"");
        sanitized = JSON_SENSITIVE_PRIMITIVE_PATTERN.matcher(sanitized).replaceAll("$1\"******\"");
        sanitized = QUERY_SENSITIVE_PATTERN.matcher(sanitized).replaceAll("$1=******");
        return sanitized;
    }

    private String resolveTraceId(ContainerRequestContext requestContext) {
        String traceId = requestContext.getHeaderString(TRACE_ID_HEADER);
        if (traceId != null && !traceId.isBlank()) {
            return traceId.trim();
        }

        return UUID.randomUUID().toString();
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
