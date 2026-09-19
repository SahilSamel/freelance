package com.freelance.alerts.service;

import com.freelance.attribute.AttributeResolutionEngine;
import com.freelance.template.seeder.AlertsBootstrap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AlertsBootstrapTest {

    @Test
    @DisplayName("Should initialize alerts bootstrap, auto-register resolvers and seed templates")
    void testBootstrapInit() {
        assertEquals("com.freelance.alerts.templates", AlertsBootstrap.MANDATED_TEMPLATE_PACKAGE);
        assertEquals("com.freelance.alerts.attribute", AlertsBootstrap.MANDATED_ATTRIBUTE_PACKAGE);

        assertDoesNotThrow(AlertsBootstrap::init);
        assertTrue(AttributeResolutionEngine.getInstance().listWhitelistedAttributes().stream()
                .anyMatch(a -> a.key().equals("USER.username")));
    }

    @Test
    @DisplayName("Private constructor reflection test")
    void testConstructor() throws Exception {
        java.lang.reflect.Constructor<AlertsBootstrap> constructor = AlertsBootstrap.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        AlertsBootstrap instance = constructor.newInstance();
        assertNotNull(instance);
    }
}