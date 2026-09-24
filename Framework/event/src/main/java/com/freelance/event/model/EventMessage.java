package com.freelance.event.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.freelance.event.IConsumer;
import com.freelance.system.SystemHardcoding;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventMessage<T> implements Comparable<EventMessage<?>>, Serializable {

    private final UUID id;
    private final String destination;
    private final String tenantId;
    private final T payload;
    private final String payloadType;
    private final EventPriority priority;
    private final UUID callerUserId;
    private final String traceId;
    private final Instant timestamp;
    private final int retryCount;
    private final long sequenceNumber;

    @JsonCreator
    public EventMessage(
            @JsonProperty("id") UUID id,
            @JsonProperty("destination") String destination,
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("payload") T payload,
            @JsonProperty("payloadType") String payloadType,
            @JsonProperty("priority") EventPriority priority,
            @JsonProperty("callerUserId") UUID callerUserId,
            @JsonProperty("traceId") String traceId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("retryCount") int retryCount,
            @JsonProperty("sequenceNumber") long sequenceNumber
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.destination = Objects.requireNonNull(destination, "destination must not be null");
        this.tenantId = (tenantId != null && !tenantId.isBlank()) ? tenantId : SystemHardcoding.DEFAULT_TENANT.asString();
        this.payload = payload;
        this.payloadType = payloadType != null ? payloadType : (payload != null ? payload.getClass().getName() : null);
        this.priority = priority != null ? priority : EventPriority.NORMAL;
        this.callerUserId = callerUserId;
        this.traceId = traceId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.retryCount = Math.max(0, retryCount);
        this.sequenceNumber = sequenceNumber;
    }

    public EventMessage(
            UUID id,
            String destination,
            T payload,
            String payloadType,
            EventPriority priority,
            UUID callerUserId,
            String traceId,
            Instant timestamp,
            int retryCount,
            long sequenceNumber
    ) {
        this(id, destination, SystemHardcoding.DEFAULT_TENANT.asString(), payload, payloadType, priority, callerUserId, traceId, timestamp, retryCount, sequenceNumber);
    }

    public EventMessage(
            UUID id,
            IConsumer destination,
            String tenantId,
            T payload,
            String payloadType,
            EventPriority priority,
            UUID callerUserId,
            String traceId,
            Instant timestamp,
            int retryCount,
            long sequenceNumber
    ) {
        this(id, destination != null ? destination.getDestination() : null, tenantId, payload, payloadType, priority, callerUserId, traceId, timestamp, retryCount, sequenceNumber);
    }

    public EventMessage(
            UUID id,
            IConsumer destination,
            T payload,
            String payloadType,
            EventPriority priority,
            UUID callerUserId,
            String traceId,
            Instant timestamp,
            int retryCount,
            long sequenceNumber
    ) {
        this(id, destination != null ? destination.getDestination() : null, SystemHardcoding.DEFAULT_TENANT.asString(), payload, payloadType, priority, callerUserId, traceId, timestamp, retryCount, sequenceNumber);
    }

    public static <T> EventMessage<T> of(IConsumer destination, T payload) {
        return new EventMessage<>(UUID.randomUUID(), destination != null ? destination.getDestination() : null, SystemHardcoding.DEFAULT_TENANT.asString(), payload, null, EventPriority.NORMAL, null, null, Instant.now(), 0, 0L);
    }

    public static <T> EventMessage<T> of(IConsumer destination, T payload, EventPriority priority) {
        return new EventMessage<>(UUID.randomUUID(), destination != null ? destination.getDestination() : null, SystemHardcoding.DEFAULT_TENANT.asString(), payload, null, priority, null, null, Instant.now(), 0, 0L);
    }

    public static <T> EventMessage<T> of(IConsumer destination, String tenantId, T payload, EventPriority priority) {
        return new EventMessage<>(UUID.randomUUID(), destination != null ? destination.getDestination() : null, tenantId, payload, null, priority, null, null, Instant.now(), 0, 0L);
    }

    public static <T> EventMessage<T> of(String destination, T payload) {
        return new EventMessage<>(UUID.randomUUID(), destination, SystemHardcoding.DEFAULT_TENANT.asString(), payload, null, EventPriority.NORMAL, null, null, Instant.now(), 0, 0L);
    }

    public static <T> EventMessage<T> of(String destination, T payload, EventPriority priority) {
        return new EventMessage<>(UUID.randomUUID(), destination, SystemHardcoding.DEFAULT_TENANT.asString(), payload, null, priority, null, null, Instant.now(), 0, 0L);
    }

    public static <T> EventMessage<T> of(String destination, String tenantId, T payload, EventPriority priority) {
        return new EventMessage<>(UUID.randomUUID(), destination, tenantId, payload, null, priority, null, null, Instant.now(), 0, 0L);
    }

    public EventMessage<T> withSequenceNumber(long seq) {
        return new EventMessage<>(this.id, this.destination, this.tenantId, this.payload, this.payloadType, this.priority, this.callerUserId, this.traceId, this.timestamp, this.retryCount, seq);
    }

    public EventMessage<T> withRetryCount(int newRetryCount) {
        return new EventMessage<>(this.id, this.destination, this.tenantId, this.payload, this.payloadType, this.priority, this.callerUserId, this.traceId, this.timestamp, newRetryCount, this.sequenceNumber);
    }

    public <R> EventMessage<R> withPayload(R newPayload) {
        return new EventMessage<>(this.id, this.destination, this.tenantId, newPayload, this.payloadType, this.priority, this.callerUserId, this.traceId, this.timestamp, this.retryCount, this.sequenceNumber);
    }

    public EventMessage<T> withTenantId(String newTenantId) {
        return new EventMessage<>(this.id, this.destination, newTenantId, this.payload, this.payloadType, this.priority, this.callerUserId, this.traceId, this.timestamp, this.retryCount, this.sequenceNumber);
    }

    public UUID getId() {
        return id;
    }

    public String getDestination() {
        return destination;
    }

    public String getTenantId() {
        return tenantId;
    }

    public T getPayload() {
        return payload;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public EventPriority getPriority() {
        return priority;
    }

    public UUID getCallerUserId() {
        return callerUserId;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public int compareTo(EventMessage<?> other) {
        if (other == null) {
            return -1;
        }

        int priorityCompare = Integer.compare(this.priority.getLevel(), other.priority.getLevel());
        if (priorityCompare != 0) {
            return priorityCompare;
        }

        int seqCompare = Long.compare(this.sequenceNumber, other.sequenceNumber);
        if (seqCompare != 0) {
            return seqCompare;
        }

        if (this.timestamp != null && other.timestamp != null) {
            return this.timestamp.compareTo(other.timestamp);
        }

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventMessage<?> that = (EventMessage<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "EventMessage{" +
                "id=" + id +
                ", destination='" + destination + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", priority=" + priority +
                ", callerUserId=" + callerUserId +
                ", traceId='" + traceId + '\'' +
                ", retryCount=" + retryCount +
                ", sequenceNumber=" + sequenceNumber +
                '}';
    }
}
