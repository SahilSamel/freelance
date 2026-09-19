package com.freelance.launcher;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MainTest {

    @Test
    void testInstantiation() {
        Main main = new Main();
        assertThat(main).isNotNull();
    }

    @Test
    void testLogResourceSummary() throws Exception {
        Method logResourceSummaryMethod = Main.class.getDeclaredMethod("logResourceSummary", Set.class);
        logResourceSummaryMethod.setAccessible(true);

        Set<Class<?>> testClasses = new HashSet<>();
        testClasses.add(Main.class);
        testClasses.add(com.freelance.config.DatabaseConfig.class);
        testClasses.add(String.class);
        testClasses.add(com.freelance.endpoint.role.RoleEndpoint.class);
        testClasses.add(com.freelance.endpoint.alerts.AlertEndpoint.class);
        testClasses.add(com.freelance.rest.jaxrs.filter.AuditFilter.class);
        testClasses.add(com.freelance.rest.jaxrs.provider.ExceptionHandler.class);

        // Should execute cleanly without exceptions
        logResourceSummaryMethod.invoke(null, testClasses);
    }
}
