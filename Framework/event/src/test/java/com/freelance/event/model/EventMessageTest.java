package com.freelance.event.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.freelance.event.TestConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventMessageTest {

    private ObjectMapper objectMapper;

    record SamplePayload(String name, int score) {}

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @DisplayName("High priority events should precede Normal and Low priority in comparison")
    void testPriorityPrecedence() {
        EventMessage<String> low = new EventMessage<>(
                UUID.randomUUID(), TestConsumer.TEST_PRIORITY_TOPIC, "low-payload", "java.lang.String",
                EventPriority.LOW, UUID.randomUUID(), "trace-low", Instant.now(), 0, 1L);

        EventMessage<String> normal = new EventMessage<>(
                UUID.randomUUID(), TestConsumer.TEST_PRIORITY_TOPIC, "norm-payload", "java.lang.String",
                EventPriority.NORMAL, UUID.randomUUID(), "trace-norm", Instant.now(), 0, 2L);

        EventMessage<String> high = new EventMessage<>(
                UUID.randomUUID(), TestConsumer.TEST_PRIORITY_TOPIC, "high-payload", "java.lang.String",
                EventPriority.HIGH, UUID.randomUUID(), "trace-high", Instant.now(), 0, 3L);

        List<EventMessage<String>> list = new ArrayList<>(List.of(low, normal, high));
        Collections.sort(list);

        assertEquals(EventPriority.HIGH, list.get(0).getPriority());
        assertEquals(EventPriority.NORMAL, list.get(1).getPriority());
        assertEquals(EventPriority.LOW, list.get(2).getPriority());
    }

    @Test
    @DisplayName("Equal priority events must maintain FIFO ordering via sequence numbers")
    void testFifoSequenceOrdering() {
        EventMessage<String> msg1 = new EventMessage<>(
                UUID.randomUUID(), TestConsumer.TEST_FIFO_TOPIC, "first", "java.lang.String",
                EventPriority.NORMAL, null, "t1", Instant.now(), 0, 100L);

        EventMessage<String> msg2 = new EventMessage<>(
                UUID.randomUUID(), TestConsumer.TEST_FIFO_TOPIC, "second", "java.lang.String",
                EventPriority.NORMAL, null, "t2", Instant.now(), 0, 101L);

        EventMessage<String> msg3 = new EventMessage<>(
                UUID.randomUUID(), TestConsumer.TEST_FIFO_TOPIC, "third", "java.lang.String",
                EventPriority.NORMAL, null, "t3", Instant.now(), 0, 102L);

        List<EventMessage<String>> list = new ArrayList<>(List.of(msg3, msg1, msg2));
        Collections.sort(list);

        assertEquals("first", list.get(0).getPayload());
        assertEquals("second", list.get(1).getPayload());
        assertEquals("third", list.get(2).getPayload());
    }

    @Test
    @DisplayName("EventMessage serialization and deserialization preserves all metadata and generic DTO payload")
    void testJsonSerialization() throws Exception {
        UUID userId = UUID.randomUUID();
        SamplePayload payload = new SamplePayload("Freelance Test", 42);

        EventMessage<SamplePayload> original = new EventMessage<>(
                UUID.randomUUID(),
                TestConsumer.TEST_PRIORITY_TOPIC,
                payload,
                SamplePayload.class.getName(),
                EventPriority.HIGH,
                userId,
                "trace-xyz-123",
                Instant.now(),
                1,
                55L
        );

        String json = objectMapper.writeValueAsString(original);
        assertNotNull(json);
        assertTrue(json.contains("Freelance Test"));
        assertTrue(json.contains("trace-xyz-123"));

        EventMessage<?> deserialized = objectMapper.readValue(json, EventMessage.class);

        assertEquals(original.getId(), deserialized.getId());
        assertEquals(original.getDestination(), deserialized.getDestination());
        assertEquals(original.getPriority(), deserialized.getPriority());
        assertEquals(original.getCallerUserId(), deserialized.getCallerUserId());
        assertEquals(original.getTraceId(), deserialized.getTraceId());
        assertEquals(original.getRetryCount(), deserialized.getRetryCount());
        assertEquals(original.getSequenceNumber(), deserialized.getSequenceNumber());
    }

    @Test
    @DisplayName("EventMessage factory methods create valid messages")
    void testFactoryMethods() {
        EventMessage<String> m1 = EventMessage.of(TestConsumer.TEST_FIFO_TOPIC, "payload1");
        assertEquals(TestConsumer.TEST_FIFO_TOPIC.getDestination(), m1.getDestination());
        assertEquals(EventPriority.NORMAL, m1.getPriority());

        EventMessage<String> m2 = EventMessage.of(TestConsumer.TEST_FIFO_TOPIC, "payload2", EventPriority.HIGH);
        assertEquals(EventPriority.HIGH, m2.getPriority());

        EventMessage<String> m3 = EventMessage.of("custom.topic", "payload3");
        assertEquals("custom.topic", m3.getDestination());

        EventMessage<String> m4 = EventMessage.of("custom.topic", "payload4", EventPriority.LOW);
        assertEquals(EventPriority.LOW, m4.getPriority());
    }

    @Test
    @DisplayName("EventMessage withSequenceNumber, withRetryCount, and withPayload produce updated immutable copies")
    void testWithMutators() {
        EventMessage<String> original = EventMessage.of("topic", "original");
        EventMessage<String> withSeq = original.withSequenceNumber(99L);
        assertEquals(99L, withSeq.getSequenceNumber());

        EventMessage<String> withRetry = original.withRetryCount(3);
        assertEquals(3, withRetry.getRetryCount());

        EventMessage<Integer> withPayload = original.withPayload(12345);
        assertEquals(12345, withPayload.getPayload());
    }

    @Test
    @DisplayName("EventMessage equals, hashCode, toString, and edge-case compareTo")
    void testEqualsHashCodeToStringAndCompare() {
        UUID id1 = UUID.randomUUID();
        Instant now = Instant.now();
        EventMessage<String> msg1 = new EventMessage<>(id1, "dest", "content", null, EventPriority.NORMAL, null, null, now, 0, 10L);
        EventMessage<String> msg2 = new EventMessage<>(id1, "dest", "content2", null, EventPriority.NORMAL, null, null, now, 0, 10L);
        EventMessage<String> msg3 = new EventMessage<>(UUID.randomUUID(), "dest", "content", null, EventPriority.NORMAL, null, null, now, 0, 10L);

        assertEquals(msg1, msg2);
        assertEquals(msg1.hashCode(), msg2.hashCode());
        assertNotEquals(msg1, msg3);
        assertNotEquals(msg1, null);
        assertNotEquals(msg1, "string");
        assertTrue(msg1.toString().contains("dest"));

        // CompareTo edge cases
        assertEquals(-1, msg1.compareTo(null));
        EventMessage<String> msgEarlier = new EventMessage<>(UUID.randomUUID(), "dest", "c", null, EventPriority.NORMAL, null, null, now.minusSeconds(10), 0, 10L);
        assertTrue(msgEarlier.compareTo(msg1) < 0);
        assertTrue(msg1.compareTo(msgEarlier) > 0);
    }

    @Test
    @DisplayName("EventHandler default onDeadLetter method logs without throwing")
    void testEventHandlerDefaultOnDeadLetter() {
        com.freelance.event.consumer.EventHandler<String> handler = msg -> {};
        EventMessage<String> msg = EventMessage.of("topic", "dead-letter-test");
        assertDoesNotThrow(() -> handler.onDeadLetter(msg, new RuntimeException("Dead letter cause")));
    }
}
