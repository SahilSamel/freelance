package com.freelance.authorization.repository;

import com.freelance.authorization.entity.Authorization;
import com.freelance.mapping.roleAuthorization.entity.RoleAuthorization;
import com.freelance.mapping.roleAuthorization.entity.RoleAuthorizationKey;
import com.freelance.mapping.roleAuthorization.repository.RoleAuthorizationRepository;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.role.entity.Role;
import com.freelance.role.repository.RoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Authorization Repository & Entity Unit Tests")
class AuthorizationRepositoryAndEntityTest {

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
                        case "contains": return true;
                        case "remove": return null;
                        case "merge": return args[0];
                        case "find":
                            if (Role.class.equals(args[0])) {
                                return new Role((UUID) args[1], "TEST_ROLE", "Desc", false);
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
                            return 2;
                        case "getResultList":
                            if (Role.QUERY_FIND_ALL.equals(lastNamedQuery) || Role.QUERY_FIND_ALL_SYSTEM_ROLES.equals(lastNamedQuery)) {
                                return List.of(new Role(UUID.randomUUID(), "ROLE_ADMIN", "Admin Role", true));
                            }
                            if (Authorization.QUERY_FIND_ALL.equals(lastNamedQuery)) {
                                return List.of(new Authorization(UUID.randomUUID(), "USER.READ", "Read User", "Desc", "User"));
                            }
                            if (Authorization.QUERY_FIND_ALL_KEYS.equals(lastNamedQuery)) {
                                return List.of("USER.READ", "USER.WRITE");
                            }
                            if (RoleAuthorization.QUERY_FIND_ALL_WITH_DETAILS.equals(lastNamedQuery)
                                    || RoleAuthorization.QUERY_FIND_BY_ROLE_ID.equals(lastNamedQuery)
                                    || RoleAuthorization.QUERY_FIND_ALL_SYSTEM_ROLE_MAPPINGS.equals(lastNamedQuery)) {
                                Role r = new Role(UUID.randomUUID(), "ROLE_SUPER", "Super", true);
                                Authorization a = new Authorization(UUID.randomUUID(), "ALL", "All", "All", "All");
                                return List.of(new RoleAuthorization(r, a));
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
    @DisplayName("RoleRepository methods")
    void testRoleRepository() {
        try (var tx = Transaction.start()) {
            RoleRepository repo = new RoleRepository();

            assertNotNull(repo.findById(UUID.randomUUID()));
            assertTrue(repo.findById(null).isEmpty());

            assertNull(repo.findByName(null));
            assertNull(repo.findByName("   "));

            assertFalse(repo.existsByName(null));
            assertFalse(repo.existsByName(""));

            List<Role> all = repo.findAll();
            assertEquals(1, all.size());
            assertEquals(Role.QUERY_FIND_ALL, lastNamedQuery);

            List<Role> systemRoles = repo.findAllSystemRoles();
            assertEquals(1, systemRoles.size());
            assertEquals(Role.QUERY_FIND_ALL_SYSTEM_ROLES, lastNamedQuery);

            Map<String, Role> map = repo.findAllSystemRolesAsMap();
            assertEquals(1, map.size());
            assertTrue(map.containsKey("ROLE_ADMIN"));

            int deleted = repo.deleteSystemRoles();
            assertEquals(2, deleted);
            assertEquals(Role.QUERY_DELETE_SYSTEM_ROLES, lastNamedQuery);
        }
    }

    @Test
    @DisplayName("AuthorizationRepository methods")
    void testAuthorizationRepository() {
        try (var tx = Transaction.start()) {
            AuthorizationRepository repo = new AuthorizationRepository();

            Set<String> keys = repo.findAllKeys();
            assertEquals(2, keys.size());
            assertTrue(keys.contains("USER.READ"));
            assertEquals(Authorization.QUERY_FIND_ALL_KEYS, lastNamedQuery);

            Map<String, Authorization> map = repo.findAllAsMap();
            assertEquals(1, map.size());
            assertTrue(map.containsKey("USER.READ"));

            assertNull(repo.findByKey(null));

            List<Authorization> all = repo.findAll();
            assertEquals(1, all.size());
        }
    }

    @Test
    @DisplayName("RoleAuthorizationRepository methods")
    void testRoleAuthorizationRepository() {
        try (var tx = Transaction.start()) {
            RoleAuthorizationRepository repo = new RoleAuthorizationRepository();

            Map<UUID, List<RoleAuthorization>> grouped = repo.getAllMappingsGroupedByRoleId();
            assertEquals(1, grouped.size());
            assertEquals(RoleAuthorization.QUERY_FIND_ALL_WITH_DETAILS, lastNamedQuery);

            List<RoleAuthorization> byRoleNull = repo.getRoleAuthorizationsByRoleId(null);
            assertTrue(byRoleNull.isEmpty());

            UUID roleId = UUID.randomUUID();
            List<RoleAuthorization> byRole = repo.getRoleAuthorizationsByRoleId(roleId);
            assertEquals(1, byRole.size());
            assertEquals(roleId, lastParams.get("roleId"));

            int deleted = repo.deleteBySystemRoles();
            assertEquals(2, deleted);
            assertEquals(RoleAuthorization.QUERY_DELETE_BY_SYSTEM_ROLES, lastNamedQuery);

            Map<RoleAuthorizationKey, RoleAuthorization> sysMap = repo.findAllSystemRoleMappings();
            assertEquals(1, sysMap.size());

            assertDoesNotThrow(() -> repo.removeByRoleId(null));
            assertDoesNotThrow(() -> repo.removeByRoleId(roleId));
        }
    }

    @Test
    @DisplayName("Entities and Key getters, setters, equals, and hashCode")
    void testEntitiesAndKey() throws Exception {
        UUID roleId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();

        Role role = new Role();
        role.setId(roleId);
        role.setName("ROLE_MGR");
        role.setDescription("Manager");
        role.setSystem(true);

        assertEquals(roleId, role.getId());
        assertEquals("ROLE_MGR", role.getName());
        assertEquals("Manager", role.getDescription());
        assertTrue(role.isSystem());

        Authorization auth = new Authorization();
        auth.setId(authId);
        auth.setKey("REPORT.VIEW");
        auth.setName("View Reports");
        auth.setDescription("Allows viewing reports");
        auth.setGroupPath("Analytics");

        assertEquals(authId, auth.getId());
        assertEquals("REPORT.VIEW", auth.getKey());
        assertEquals("View Reports", auth.getName());
        assertEquals("Allows viewing reports", auth.getDescription());
        assertEquals("Analytics", auth.getGroupPath());

        RoleAuthorizationKey key1 = new RoleAuthorizationKey();
        key1.setRoleId(roleId);
        key1.setAuthorizationId(authId);

        RoleAuthorizationKey key2 = new RoleAuthorizationKey(roleId, authId);
        RoleAuthorizationKey key3 = new RoleAuthorizationKey(UUID.randomUUID(), authId);

        assertEquals(key1, key2);
        assertNotEquals(key1, key3);
        assertNotEquals(key1, null);
        assertNotEquals(key1, "different-type");
        assertEquals(key1.hashCode(), key2.hashCode());
        assertEquals(roleId, key1.getRoleId());
        assertEquals(authId, key1.getAuthorizationId());

        RoleAuthorization ra = new RoleAuthorization();
        ra.setId(key1);
        ra.setRole(role);
        ra.setAuthorization(auth);
        LocalDateTime now = LocalDateTime.now();
        ra.setCreatedDate(now);

        assertEquals(key1, ra.getId());
        assertEquals(role, ra.getRole());
        assertEquals(auth, ra.getAuthorization());
        assertEquals(now, ra.getCreatedDate());

        RoleAuthorization raNull = new RoleAuthorization(null, null);
        assertNull(raNull.getRole());
        assertNull(raNull.getAuthorization());

        // Test @PrePersist onCreate
        RoleAuthorization raNew = new RoleAuthorization(role, auth);
        assertNull(raNew.getCreatedDate());
        Method onCreateMethod = RoleAuthorization.class.getDeclaredMethod("onCreate");
        onCreateMethod.setAccessible(true);
        onCreateMethod.invoke(raNew);
        assertNotNull(raNew.getCreatedDate());
    }
}
