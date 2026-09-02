package com.freelance.event.broker;

import com.freelance.ConfigurationEngine;
import com.freelance.event.broker.impl.InMemoryEventBroker;
import com.freelance.event.broker.impl.RedisEventBroker;
import com.freelance.keys.event.EventConfigKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EventBrokerFactory {

    private static final Logger logger = LoggerFactory.getLogger(EventBrokerFactory.class);

    private EventBrokerFactory() {
    }

    public static EventBroker createBroker() {
        String brokerTypeStr = ConfigurationEngine.get(EventConfigKeys.EVENT_BROKER_TYPE);
        EventBrokerType brokerType = EventBrokerType.fromString(brokerTypeStr);

        logger.info("[EventBrokerFactory] Initializing event broker with driver type: '{}'", brokerType);

        switch (brokerType) {
            case REDIS -> {
                String host = ConfigurationEngine.get(EventConfigKeys.REDIS_HOST);
                int port = ConfigurationEngine.getInt(EventConfigKeys.REDIS_PORT);
                String password = ConfigurationEngine.get(EventConfigKeys.REDIS_PASSWORD);
                int database = ConfigurationEngine.getInt(EventConfigKeys.REDIS_DATABASE);

                return new RedisEventBroker(host, port, password, database);
            }
            case IN_MEMORY -> {
                return new InMemoryEventBroker();
            }
            default -> {
                logger.warn("[EventBrokerFactory] Unrecognized broker type '{}', defaulting to IN_MEMORY", brokerType);
                return new InMemoryEventBroker();
            }
        }
    }
}
