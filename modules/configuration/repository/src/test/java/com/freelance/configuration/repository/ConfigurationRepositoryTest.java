package com.freelance.configuration.repository;

import com.freelance.configuration.dto.ErrorConfigEntry;
import com.freelance.configuration.error.entity.ErrorConfiguration;
import com.freelance.configuration.error.repository.ErrorConfigurationRepository;
import com.freelance.configuration.system.entity.SystemConfiguration;
import com.freelance.configuration.system.repository.SystemConfigurationRepository;
import com.freelance.rest.app.transaction.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Configuration Repository & Entity Unit Tests")
class ConfigurationRepositoryTest {

    private String lastNamedQuery;
    private final Map<String, Object> lastParams = new HashMap<>();

    @BeforeEach
    void setUp() {
        lastNamedQuery = null;
        lastParams.clear();

        EntityTransaction mockTx = (EntityTransaction) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityTransaction.class},
                (proxy, method, args) -> {
                    if ("isActive".equals(method.getName())) return true;
                    return null;
                }
        );

        EntityManager mockEm = (EntityManager) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getTransaction": return mockTx;
                        case "createNamedQuery":
                            lastNamedQuery = (String) args[0];
                            return createMockQuery();
                        case "find":
                            if (SystemConfiguration.class.equals(args[0])) {
                                return new SystemConfiguration((String) args[1], "value", "desc", "valid");
                            }
                            if (ErrorConfiguration.class.equals(args[0])) {
                                return new ErrorConfiguration((String) args[1], 400, "Bad Request", "desc");
                            }
                            return null;
                        case "close": return null;
                        default: return null;
                    }
                }
        );

        EntityManagerFactory mockEmf = (EntityManagerFactory) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityManagerFactory.class},
                (proxy, method, args) -> {
                    if ("createEntityManager".equals(method.getName())) return mockEm;
                    return null;
                }
        );

        Transaction.init(mockEmf);
    }

    @SuppressWarnings("unchecked")
    private Object createMockQuery() {
        return Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{TypedQuery.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setParameter":
                            lastParams.put((String) args[0], args[1]);
                            return proxy;
                        case "setMaxResults":
                        case "setFirstResult":
                            return proxy;
                        case "executeUpdate":
                            return 3;
                        case "getResultList":
                            if (SystemConfiguration.QUERY_FIND_ALL.equals(lastNamedQuery)) {
                                return List.of(new SystemConfiguration("CFG_KEY", "CFG_VAL", "Desc", "REGEX"));
                            }
                            if (SystemConfiguration.QUERY_FIND_ALL_IDS.equals(lastNamedQuery)) {
                                return List.of("CFG_KEY");
                            }
                            if (ErrorConfiguration.QUERY_FIND_ALL.equals(lastNamedQuery)) {
                                return List.of(new ErrorConfiguration("ERR_404", 404, "Not Found", "Desc"));
                            }
                            return Collections.emptyList();
                        default: return null;
                    }
                }
        );
    }

    @AfterEach
    void tearDown() {
        try {
            Transaction.finish();
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("SystemConfigurationRepository methods")
    void testSystemConfigurationRepository() {
        try (var tx = Transaction.start()) {
            SystemConfigurationRepository repo = new SystemConfigurationRepository();

            Map<String, String> all = repo.getAll();
            assertEquals(1, all.size());
            assertEquals("CFG_VAL", all.get("CFG_KEY"));

            Set<String> ids = repo.findAllIds();
            assertEquals(1, ids.size());
            assertTrue(ids.contains("CFG_KEY"));

            int deleted = repo.deleteSystemOwned();
            assertEquals(3, deleted);
            assertEquals(SystemConfiguration.QUERY_DELETE_SYSTEM_OWNED, lastNamedQuery);

            Map<String, SystemConfiguration> map = repo.findAllEntitiesAsMap();
            assertEquals(1, map.size());

            List<SystemConfiguration> list = repo.findAllEntities();
            assertEquals(1, list.size());

            SystemConfiguration entity = repo.get("CFG_KEY");
            assertNotNull(entity);
            assertEquals("CFG_KEY", entity.getConfigId());
        }
    }

    @Test
    @DisplayName("ErrorConfigurationRepository methods")
    void testErrorConfigurationRepository() {
        try (var tx = Transaction.start()) {
            ErrorConfigurationRepository repo = new ErrorConfigurationRepository();

            Map<String, ErrorConfigEntry> entries = repo.getAllErrorEntries();
            assertEquals(1, entries.size());
            assertEquals(404, entries.get("ERR_404").statusCode());
            assertEquals("Not Found", entries.get("ERR_404").errorMessage());

            Map<String, ErrorConfiguration> map = repo.findAllEntitiesAsMap();
            assertEquals(1, map.size());

            List<ErrorConfiguration> list = repo.findAllEntities();
            assertEquals(1, list.size());

            ErrorConfiguration entity = repo.get("ERR_404");
            assertNotNull(entity);
            assertEquals("ERR_404", entity.getErrorId());
        }
    }

    @Test
    @DisplayName("SystemConfiguration and ErrorConfiguration entity getters and setters")
    void testEntities() {
        SystemConfiguration sys1 = new SystemConfiguration();
        sys1.setConfigId("ID1");
        sys1.setConfigValue("VAL1");
        sys1.setDescription("DESC1");
        sys1.setValidation("VAL_RULE");

        assertEquals("ID1", sys1.getConfigId());
        assertEquals("VAL1", sys1.getConfigValue());
        assertEquals("DESC1", sys1.getDescription());
        assertEquals("VAL_RULE", sys1.getValidation());

        SystemConfiguration sys2 = new SystemConfiguration("ID2", "VAL2", "DESC2");
        assertEquals("ID2", sys2.getConfigId());
        assertEquals("VAL2", sys2.getConfigValue());
        assertEquals("DESC2", sys2.getDescription());
        assertNull(sys2.getValidation());

        ErrorConfiguration err = new ErrorConfiguration();
        err.setErrorId("ERR_1");
        err.setStatusCode(500);
        err.setErrorMessage("Internal Error");
        err.setDescription("Server error description");

        assertEquals("ERR_1", err.getErrorId());
        assertEquals(500, err.getStatusCode());
        assertEquals("Internal Error", err.getErrorMessage());
        assertEquals("Server error description", err.getDescription());
    }
}
