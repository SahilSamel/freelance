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
        String brokerType = ConfigurationEngine.get(EventConfigKeys.EVENT_BROKER_TYPE);
        if (brokerType == null || brokerType.isBlank()) {
            brokerType = EventConfigKeys.EVENT_BROKER_TYPE.getDefaultValue();
        }

        logger.info("[EventBrokerFactory] Initializing event broker with driver type: '{}'", brokerType);

        if ("REDIS".equalsIgnoreCase(brokerType.trim())) {
            String host = ConfigurationEngine.get(EventConfigKeys.REDIS_HOST);
            int port = ConfigurationEngine.getInt(EventConfigKeys.REDIS_PORT);
            String password = ConfigurationEngine.get(EventConfigKeys.REDIS_PASSWORD);
            int database = ConfigurationEngine.getInt(EventConfigKeys.REDIS_DATABASE);

            return new RedisEventBroker(host, port, password, database);
        }

        return new InMemoryEventBroker();
    }
}
