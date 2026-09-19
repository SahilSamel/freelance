package com.freelance.jpa;

import com.freelance.config.DatabaseConfig;
import com.freelance.enums.DeploymentMode;
import com.freelance.flyway.FlywayMigrationRunner;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Framework Database Unit Tests")
class JpaFactoryAndDatabaseTest {

    @Entity
    static class DummyEntity {
        @Id
        private Long id;
    }

    @Test
    @DisplayName("DatabaseConfig.fromArgs parses valid arguments with mode and default mode")
    void testDatabaseConfigFromArgs() {
        // 4 args
        String[] args4 = {"jdbc:postgresql://localhost:5432/db", "user", "pass", "TEST"};
        DatabaseConfig config4 = DatabaseConfig.fromArgs(args4);
        assertNotNull(config4);
        assertEquals("jdbc:postgresql://localhost:5432/db", config4.url());
        assertEquals("user", config4.username());
        assertEquals("pass", config4.password());
        assertEquals(DeploymentMode.TEST, config4.mode());

        // 3 args (defaults to DEV)
        String[] args3 = {"jdbc:postgresql://localhost:5432/db", "user", "pass"};
        DatabaseConfig config3 = DatabaseConfig.fromArgs(args3);
        assertNotNull(config3);
        assertEquals(DeploymentMode.DEV, config3.mode());

        // PROD mode
        String[] argsProd = {"jdbc:postgresql://localhost:5432/db", "user", "pass", "prod"};
        DatabaseConfig configProd = DatabaseConfig.fromArgs(argsProd);
        assertEquals(DeploymentMode.PROD, configProd.mode());

        // Insufficient args throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> DatabaseConfig.fromArgs(new String[]{"url", "user"}));
        assertThrows(IllegalArgumentException.class, () -> DatabaseConfig.fromArgs(new String[0]));
        assertThrows(NullPointerException.class, () -> DatabaseConfig.fromArgs(null));

        // 3-param constructor
        DatabaseConfig config3Param = new DatabaseConfig("url", "user", "pass");
        assertEquals(DeploymentMode.DEV, config3Param.mode());
    }

    @Test
    @DisplayName("EntityScanner scans for JPA entities in given package")
    void testEntityScanner() {
        EntityScanner scanner = new EntityScanner();
        Set<Class<?>> empty = scanner.scanEntities("com.freelance.nonexistent.package");
        assertNotNull(empty);
        assertTrue(empty.isEmpty());

        Set<Class<?>> found = scanner.scanEntities("com.freelance.jpa");
        assertNotNull(found);
        assertTrue(found.contains(DummyEntity.class));
    }

    @Test
    @DisplayName("JpaFactory throws IllegalStateException when no entities are found")
    void testJpaFactoryNoEntitiesThrows() {
        JpaFactory factory = new JpaFactory();
        DatabaseConfig config = new DatabaseConfig("jdbc:postgresql://localhost:5432/db", "user", "pass", DeploymentMode.TEST);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                factory.createEntityManagerFactory(config, "com.freelance.nonexistent.package"));
        assertTrue(ex.getMessage().contains("No JPA entities found in package"));
    }

    @Test
    @DisplayName("JpaFactory.logEntitySummary logs root and relative package breakdowns correctly")
    void testLogEntitySummary() throws Exception {
        JpaFactory factory = new JpaFactory();

        Method logMethod = JpaFactory.class.getDeclaredMethod("logEntitySummary", String.class, Set.class);
        logMethod.setAccessible(true);

        Set<Class<?>> entities = Set.of(DummyEntity.class);
        assertDoesNotThrow(() -> logMethod.invoke(factory, "com.freelance", entities));
        assertDoesNotThrow(() -> logMethod.invoke(factory, "com.freelance.jpa", entities));
    }

    @Test
    @DisplayName("FlywayMigrationRunner instantiation")
    void testFlywayMigrationRunnerInstantiation() {
        FlywayMigrationRunner runner = new FlywayMigrationRunner();
        assertNotNull(runner);
    }
}
