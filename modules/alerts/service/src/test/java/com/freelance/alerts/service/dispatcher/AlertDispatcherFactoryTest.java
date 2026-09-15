package com.freelance.alerts.service.dispatcher;

import com.freelance.alerts.AlertChannel;
import com.freelance.dispatcher.AlertDispatcherFactory;
import com.freelance.dispatcher.IAlertDispatcher;
import com.freelance.dispatcher.impl.whatsapp.WhatsAppDispatcherService;
import com.freelance.fixtures.TestContextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AlertDispatcherFactoryTest {

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should contain pre-registered dispatcher for WhatsApp by default")
    void testDefaultDispatchers() {
        AlertDispatcherFactory factory = new AlertDispatcherFactory();

        assertTrue(factory.hasDispatcher(AlertChannel.WHATSAPP));
        assertEquals(AlertChannel.WHATSAPP, factory.getDispatcher(AlertChannel.WHATSAPP).orElseThrow().getChannel());
    }

    @Test
    @DisplayName("Should allow dynamically registering and unregistering dispatchers")
    void testDynamicRegistrationAndUnregistration() {
        AlertDispatcherFactory factory = new AlertDispatcherFactory();

        factory.unregister(AlertChannel.WHATSAPP);
        assertFalse(factory.hasDispatcher(AlertChannel.WHATSAPP));

        // Register custom dispatcher for WhatsApp
        factory.register(new WhatsAppDispatcherService());
        assertTrue(factory.hasDispatcher(AlertChannel.WHATSAPP));
    }

    @Test
    @DisplayName("Should fallback to configured default channel when requested channel is missing")
    void testFallbackToDefaultChannel() {
        AlertDispatcherFactory factory = new AlertDispatcherFactory();

        // When passing null, it falls back to AlertConfigKeys.DEFAULT_ALERT_CHANNEL (WHATSAPP)
        Optional<IAlertDispatcher> dispatcherOpt = factory.getDispatcher(null);
        assertTrue(dispatcherOpt.isPresent(), "Should fall back to default channel");
        assertEquals(AlertChannel.WHATSAPP, dispatcherOpt.get().getChannel());
    }

    @Test
    @DisplayName("Should return empty optional when neither requested nor default channel is available")
    void testNoDispatcherAvailable() {
        AlertDispatcherFactory factory = new AlertDispatcherFactory();
        factory.unregister(AlertChannel.WHATSAPP);

        Optional<IAlertDispatcher> dispatcherOpt = factory.getDispatcher(AlertChannel.WHATSAPP);
        assertTrue(dispatcherOpt.isEmpty());
    }
}
