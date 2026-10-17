package com.freelance.alerts.attribute;

import com.freelance.fixtures.FakeUserRepository;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserAttributeResolver Tests")
class UserAttributeResolverTest {

    private FakeUserRepository userRepository;
    private UserAttributeResolver resolver;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
        userRepository = new FakeUserRepository();
        resolver = new UserAttributeResolver(userRepository);
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("getDomain returns USER")
    void testGetDomain() {
        assertEquals("USER", resolver.getDomain());
    }

    @Test
    @DisplayName("getSupportedAttributes returns valid attribute keys")
    void testGetSupportedAttributes() {
        Set<String> supported = resolver.getSupportedAttributes();
        assertFalse(supported.contains("USER.username"));
        assertFalse(supported.contains("USER.user_id"));
        assertTrue(supported.contains("USER.first_name"));
        assertTrue(supported.contains("USER.name"));
    }

    @Test
    @DisplayName("getAttributeDescription returns descriptions for known keys")
    void testGetAttributeDescription() {
        assertNotNull(resolver.getAttributeDescription("USER.first_name"));
        assertNotNull(resolver.getAttributeDescription("USER.name"));
        assertNull(resolver.getAttributeDescription("UNKNOWN.key"));
    }

    @Test
    @DisplayName("resolve with null or empty parameters returns empty map")
    void testResolveEmptyParameters() {
        assertTrue(resolver.resolve(null, Set.of("USER.first_name")).isEmpty());
        assertTrue(resolver.resolve(UUID.randomUUID(), null).isEmpty());
        assertTrue(resolver.resolve(UUID.randomUUID(), Collections.emptySet()).isEmpty());
    }

    @Test
    @DisplayName("resolve resolves USER.first_name and USER.name")
    void testResolveUserAttributes() {
        UUID userId = UUID.randomUUID();
        User user = new User("John Doe", "HashPassword123");
        user.setId(userId);
        userRepository.persist(user);

        Set<String> requested = new HashSet<>(Arrays.asList("USER.first_name", "USER.name", "OTHER.ignored", null));
        Map<String, String> resolved = resolver.resolve(userId, requested);

        assertEquals("John", resolved.get("USER.first_name"));
        assertEquals("John Doe", resolved.get("USER.name"));
        assertNull(resolved.get("OTHER.ignored"));
    }

    @Test
    @DisplayName("resolve with single-word username returns entire word for first_name")
    void testResolveSingleWordFirstName() {
        UUID userId = UUID.randomUUID();
        User user = new User("Alice", "HashPassword123");
        user.setId(userId);
        userRepository.persist(user);

        Map<String, String> resolved = resolver.resolve(userId, Set.of("USER.first_name"));
        assertEquals("Alice", resolved.get("USER.first_name"));
    }

    @Test
    @DisplayName("resolve when user not found in repository returns empty map")
    void testResolveUserNotFound() {
        UUID userId = UUID.randomUUID();
        Map<String, String> resolved = resolver.resolve(userId, Set.of("USER.first_name", "USER.name"));
        assertTrue(resolved.isEmpty());
    }

    @Test
    @DisplayName("resolve with member username starting with mem_ resolves first_name and name from User entity fields")
    void testMemberUserWithDistinctFirstNameAndLastName() {
        UUID userId = UUID.randomUUID();
        User user = new User("mem_8693850666", "HashPassword123", "Tanisha", "Sharma");
        user.setId(userId);
        userRepository.persist(user);

        Set<String> requested = Set.of("USER.first_name", "USER.name");
        Map<String, String> resolved = resolver.resolve(userId, requested);

        assertEquals("Tanisha", resolved.get("USER.first_name"));
        assertEquals("Tanisha Sharma", resolved.get("USER.name"));
    }

    @Test
    @DisplayName("No-arg constructor instantiates resolver")
    void testNoArgConstructor() {
        UserAttributeResolver defaultResolver = new UserAttributeResolver();
        assertEquals("USER", defaultResolver.getDomain());
    }
}
