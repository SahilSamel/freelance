package com.freelance.event.engine;

import com.freelance.event.IConsumer;
import com.freelance.event.model.EventMessage;
import com.freelance.event.model.EventPriority;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.system.SystemHardcoding;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Static publishing utility for emitting application domain events.
 */
public final class EventManagementEngine {

    private static final String MDC_TRACE_ID = "traceId";

    private EventManagementEngine() {
    }

    public static <T> void emit(IConsumer destination, T payload, EventPriority priority) {
        String tenantId = null;
        try {
            if (Transaction.getRequestContext() != null) {
                tenantId = Transaction.getRequestContext().tenantId();
            }
        } catch (Exception ignored) {
            // No active transaction context on current thread
        }
        emit(destination, tenantId, payload, priority);
    }

    public static <T> void emit(IConsumer destination, String tenantId, T payload, EventPriority priority) {
        Objects.requireNonNull(destination, "destination must not be null");

        String resolvedTenant = (tenantId != null && !tenantId.isBlank())
                ? tenantId
                : SystemHardcoding.DEFAULT_TENANT.asString();

        UUID callerUserId = SystemHardcoding.SYSTEM_USER_ID.asUuid();
        try {
            if (Transaction.getRequestContext() != null && Transaction.getRequestContext().userId() != null) {
                callerUserId = Transaction.getRequestContext().userId();
            }
        } catch (Exception ignored) {
            // Fallback to SYSTEM_USER_ID
        }

        String traceId = MDC.get(MDC_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        EventMessage<T> message = new EventMessage<>(
                UUID.randomUUID(),
                destination.getDestination(),
                resolvedTenant,
                payload,
                payload != null ? payload.getClass().getName() : null,
                priority != null ? priority : EventPriority.NORMAL,
                callerUserId,
                traceId,
                Instant.now(),
                0,
                0L
        );

        EventRegistry.getInstance().getOrCreateBroker().publish(message);
    }
}
