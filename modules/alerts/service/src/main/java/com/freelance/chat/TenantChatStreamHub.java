package com.freelance.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Thread-safe multi-tenant Server-Sent Events (SSE) hub for real-time chat updates.
 * Manages active client sinks partitioned strictly by tenantId.
 */
public class TenantChatStreamHub {

    private static final Logger logger = LoggerFactory.getLogger(TenantChatStreamHub.class);

    private static final Map<String, Set<SseEventSink>> TENANT_SINKS = new ConcurrentHashMap<>();
    private static volatile Sse sseInstance;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final ScheduledExecutorService HEARTBEAT_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "chat-sse-heartbeat");
        t.setDaemon(true);
        return t;
    });

    static {
        // Ping all open connections every 30 seconds to prevent Nginx/browser timeouts
        HEARTBEAT_EXECUTOR.scheduleWithFixedDelay(TenantChatStreamHub::sendHeartbeats, 30, 30, TimeUnit.SECONDS);
    }

    private TenantChatStreamHub() {}

    /**
     * Registers an SSE event sink for a given tenant.
     *
     * @param tenantId the tenant identifier
     * @param sink     the client's SSE event sink
     * @param sse      the JAX-RS SSE builder factory
     */
    public static void register(String tenantId, SseEventSink sink, Sse sse) {
        if (tenantId == null || sink == null || sse == null) return;
        sseInstance = sse;

        String key = tenantId.trim().toLowerCase();
        Set<SseEventSink> sinks = TENANT_SINKS.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
        sinks.add(sink);

        logger.info("[TenantChatStreamHub] Registered SSE client for tenant '{}'. Active connections for tenant: {}", key, sinks.size());

        try {
            OutboundSseEvent connectEvent = sse.newEventBuilder()
                    .name("CONNECTED")
                    .data(String.class, "{\"status\":\"CONNECTED\",\"tenantId\":\"" + key + "\"}")
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .build();
            sink.send(connectEvent);
        } catch (Exception e) {
            logger.warn("[TenantChatStreamHub] Failed to send initial CONNECTED event to client: {}", e.getMessage());
            sinks.remove(sink);
            try {
                sink.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Broadcasts an event and JSON payload to all active client sinks for a specific tenant.
     *
     * @param tenantId  target tenant identifier
     * @param eventType name of the SSE event (e.g., "CHAT_MESSAGE", "MESSAGE_STATUS")
     * @param payload   data object to serialize to JSON
     */
    public static void broadcast(String tenantId, String eventType, Object payload) {
        if (tenantId == null || payload == null || sseInstance == null) return;

        String key = tenantId.trim().toLowerCase();
        Set<SseEventSink> sinks = TENANT_SINKS.get(key);
        if (sinks == null || sinks.isEmpty()) {
            return;
        }

        try {
            String json = OBJECT_MAPPER.writeValueAsString(payload);
            OutboundSseEvent event = sseInstance.newEventBuilder()
                    .name(eventType)
                    .data(String.class, json)
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .build();

            for (SseEventSink sink : sinks) {
                if (sink.isClosed()) {
                    sinks.remove(sink);
                    continue;
                }
                try {
                    sink.send(event).whenComplete((res, err) -> {
                        if (err != null) {
                            sinks.remove(sink);
                            try {
                                sink.close();
                            } catch (Exception ignored) {}
                        }
                    });
                } catch (Exception e) {
                    sinks.remove(sink);
                    try {
                        sink.close();
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            logger.error("[TenantChatStreamHub] Failed to serialize or broadcast event '{}' for tenant '{}': {}", eventType, key, e.getMessage());
        }
    }

    /**
     * Periodically sends SSE comments to all active connections to keep idle TCP sockets alive through proxies.
     */
    private static void sendHeartbeats() {
        if (sseInstance == null) return;

        try {
            OutboundSseEvent pingEvent = sseInstance.newEventBuilder()
                    .comment("ping")
                    .build();

            for (Map.Entry<String, Set<SseEventSink>> entry : TENANT_SINKS.entrySet()) {
                Set<SseEventSink> sinks = entry.getValue();
                sinks.removeIf(sink -> {
                    if (sink.isClosed()) {
                        return true;
                    }
                    try {
                        sink.send(pingEvent);
                        return false;
                    } catch (Exception e) {
                        try {
                            sink.close();
                        } catch (Exception ignored) {}
                        return true;
                    }
                });
            }
        } catch (Exception e) {
            logger.debug("[TenantChatStreamHub] Heartbeat ping skipped: {}", e.getMessage());
        }
    }

    /**
     * Returns the number of active SSE client connections for a given tenant.
     */
    public static int getActiveConnectionsCount(String tenantId) {
        if (tenantId == null) return 0;
        Set<SseEventSink> sinks = TENANT_SINKS.get(tenantId.trim().toLowerCase());
        return sinks != null ? sinks.size() : 0;
    }
}
