package com.freelance.event.engine;

import com.freelance.event.TestConsumer;
import com.freelance.event.broker.impl.InMemoryEventBroker;
import com.freelance.event.consumer.ConsumerConfig;
import com.freelance.event.model.EventMessage;
import com.freelance.event.model.EventPriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TransactionAndIdentityPropagationTest {

    private InMemoryEventBroker broker;

    record UserNotificationDTO(UUID targetUserId, String message) {}

    @BeforeEach
    void setUp() {
        broker = new InMemoryEventBroker();
        EventRegistry.getInstance().setBrokerForTesting(broker);
    }

    @AfterEach
    void tearDown() {
        EventRegistry.getInstance().shutdown(1000);
    }

    @Test
    @DisplayName("Caller userId, tenantId, and MDC traceId are preserved and propagated to the worker thread")
    void testCallerIdentityAndMdcPropagation() throws InterruptedException {
        TestConsumer destination = TestConsumer.TEST_IDENTITY_TOPIC;
        UUID expectedCallerId = UUID.randomUUID();
        String expectedTraceId = "trace-custom-abc-987";
        String expectedTenantId = "tenant_megacorp";
        UserNotificationDTO expectedPayload = new UserNotificationDTO(UUID.randomUUID(), "Welcome aboard!");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedTraceId = new AtomicReference<>();
        AtomicReference<UUID> receivedCallerId = new AtomicReference<>();
        AtomicReference<String> receivedTenantId = new AtomicReference<>();
        AtomicReference<UserNotificationDTO> receivedPayload = new AtomicReference<>();

        EventRegistry.getInstance().registerConsumer(
                destination,
                ConsumerConfig.builder().concurrency(1).build(),
                UserNotificationDTO.class,
                msg -> {
                    receivedTraceId.set(MDC.get("traceId"));
                    receivedCallerId.set(msg.getCallerUserId());
                    receivedTenantId.set(msg.getTenantId());
                    receivedPayload.set(msg.getPayload());
                    latch.countDown();
                }
        );

        EventMessage<UserNotificationDTO> envelope = new EventMessage<>(
                UUID.randomUUID(),
                destination.getDestination(),
                expectedTenantId,
                expectedPayload,
                UserNotificationDTO.class.getName(),
                EventPriority.HIGH,
                expectedCallerId,
                expectedTraceId,
                Instant.now(),
                0,
                0L
        );

        broker.publish(envelope);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Event worker should execute task within 5 seconds");

        assertEquals(expectedTraceId, receivedTraceId.get(), "Worker MDC traceId must match caller traceId");
        assertEquals(expectedCallerId, receivedCallerId.get(), "Worker message callerUserId must match caller");
        assertEquals(expectedTenantId, receivedTenantId.get(), "Worker message tenantId must match caller tenantId");
        assertNotNull(receivedPayload.get());
        assertEquals(expectedPayload.message(), receivedPayload.get().message());
    }

    @Test
    @DisplayName("emit with explicit tenantId populates tenantId in EventMessage")
    void testEmitWithExplicitTenant() throws InterruptedException {
        TestConsumer destination = TestConsumer.TEST_IDENTITY_TOPIC;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedTenantId = new AtomicReference<>();

        EventRegistry.getInstance().registerConsumer(
                destination,
                ConsumerConfig.builder().concurrency(1).build(),
                String.class,
                msg -> {
                    receivedTenantId.set(msg.getTenantId());
                    latch.countDown();
                }
        );

        EventManagementEngine.emit(destination, "tenant_finance", "payload", EventPriority.NORMAL);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("tenant_finance", receivedTenantId.get());
    }
}
