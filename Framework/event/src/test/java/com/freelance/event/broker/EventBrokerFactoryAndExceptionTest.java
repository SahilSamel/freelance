package com.freelance.event.broker;

import com.freelance.ConfigurationEngine;
import com.freelance.event.broker.impl.InMemoryEventBroker;
import com.freelance.event.broker.impl.RedisEventBroker;
import com.freelance.event.exception.EventDelayedException;
import com.freelance.keys.event.EventConfigKeys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EventBrokerFactory & EventDelayedException Unit Tests")
public class EventBrokerFactoryAndExceptionTest {

    @Test
    @DisplayName("EventDelayedException constructors and delay accessors")
    void testEventDelayedException() {
        EventDelayedException ex1 = new EventDelayedException("Wait for reply");
        assertEquals("Wait for reply", ex1.getMessage());
        assertNull(ex1.getCustomDelayMillis());

        EventDelayedException ex2 = new EventDelayedException("Wait for human", 5000L);
        assertEquals("Wait for human", ex2.getMessage());
        assertEquals(5000L, ex2.getCustomDelayMillis());

        NullPointerException cause = new NullPointerException("nested");
        EventDelayedException ex3 = new EventDelayedException("Failed with cause", cause);
        assertEquals("Failed with cause", ex3.getMessage());
        assertEquals(cause, ex3.getCause());
        assertNull(ex3.getCustomDelayMillis());

        EventDelayedException ex4 = new EventDelayedException("Delayed with cause", 10000L, cause);
        assertEquals("Delayed with cause", ex4.getMessage());
        assertEquals(10000L, ex4.getCustomDelayMillis());
        assertEquals(cause, ex4.getCause());
    }

    @Test
    @DisplayName("EventBrokerType enum parsing")
    void testEventBrokerType() {
        assertEquals(EventBrokerType.IN_MEMORY, EventBrokerType.fromString("IN_MEMORY"));
        assertEquals(EventBrokerType.IN_MEMORY, EventBrokerType.fromString("in_memory"));
        assertEquals(EventBrokerType.REDIS, EventBrokerType.fromString("REDIS"));
        assertEquals(EventBrokerType.REDIS, EventBrokerType.fromString("redis"));
        assertEquals(EventBrokerType.IN_MEMORY, EventBrokerType.fromString(null));
        assertEquals(EventBrokerType.IN_MEMORY, EventBrokerType.fromString("UNKNOWN"));
    }

    @Test
    @DisplayName("EventBrokerFactory produces InMemory broker by default and when configured")
    void testFactoryCreation() {
        EventBroker broker = EventBrokerFactory.createBroker();
        assertNotNull(broker);
        assertTrue(broker instanceof InMemoryEventBroker);
        broker.shutdown(1000L);
    }

    @Test
    @DisplayName("Private constructor reflection test")
    void testConstructor() throws Exception {
        java.lang.reflect.Constructor<EventBrokerFactory> constructor = EventBrokerFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        EventBrokerFactory instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    @DisplayName("EventBrokerFactory creates Redis broker when configured")
    void testRedisFactoryCreation() {
        try {
            com.freelance.manager.CacheManager.getInstance().init("com.freelance.fixtures");
        } catch (IllegalStateException ignored) {
        }

        com.freelance.manager.CacheManager.getInstance().put(
                "cache_SystemConfig",
                EventConfigKeys.EVENT_BROKER_TYPE.getKey(),
                "REDIS"
        );
        com.freelance.manager.CacheManager.getInstance().put(
                "cache_SystemConfig",
                EventConfigKeys.REDIS_HOST.getKey(),
                "localhost"
        );
        com.freelance.manager.CacheManager.getInstance().put(
                "cache_SystemConfig",
                EventConfigKeys.REDIS_PORT.getKey(),
                "6379"
        );

        try {
            EventBroker broker = EventBrokerFactory.createBroker();
            assertNotNull(broker);
            assertTrue(broker instanceof RedisEventBroker);
            broker.shutdown(100L);
        } finally {
            ConfigurationEngine.invalidateConfig(EventConfigKeys.EVENT_BROKER_TYPE);
            ConfigurationEngine.invalidateConfig(EventConfigKeys.REDIS_HOST);
            ConfigurationEngine.invalidateConfig(EventConfigKeys.REDIS_PORT);
        }
    }
}

