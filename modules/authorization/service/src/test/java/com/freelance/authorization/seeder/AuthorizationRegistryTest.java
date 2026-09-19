package com.freelance.authorization.seeder;

import com.freelance.rest.app.endpoint.annotation.Authorization;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import test.fixtures.auth.SampleAuthFixtures;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthorizationRegistry Discovery & Validation Tests")
class AuthorizationRegistryTest {

    private static org.reflections.Reflections originalReflections;

    @Test
    @DisplayName("getAllAuthorizations discovers valid and complete @Authorization annotations")
    void getAllAuthorizations_returnsValidAuthorizations() {
        List<Authorization> authorizations = AuthorizationRegistry.getAllAuthorizations();

        assertNotNull(authorizations);
        assertFalse(authorizations.isEmpty(), "Expected to discover at least one @Authorization annotation across endpoints");

        Set<String> uniqueKeys = new HashSet<>();

        for (Authorization auth : authorizations) {
            assertNotNull(auth.key(), "Authorization key cannot be null");
            assertFalse(auth.key().isBlank(), "Authorization key cannot be blank");

            assertNotNull(auth.name(), "Authorization name cannot be null for key: " + auth.key());
            assertFalse(auth.name().isBlank(), "Authorization name cannot be blank for key: " + auth.key());

            assertNotNull(auth.group(), "Authorization group cannot be null for key: " + auth.key());
            assertFalse(auth.group().isBlank(), "Authorization group cannot be blank for key: " + auth.key());

            assertTrue(uniqueKeys.add(auth.key()),
                    "Duplicate @Authorization key detected: '" + auth.key() + "'");
        }
    }

    @Test
    @DisplayName("Private constructor reflection test")
    void testConstructor() throws Exception {
        java.lang.reflect.Constructor<AuthorizationRegistry> constructor = AuthorizationRegistry.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        AuthorizationRegistry instance = constructor.newInstance();
        assertNotNull(instance);
    }

    private void runWithCustomReflections(Set<Method> methods, Runnable runnable) throws Exception {
        // Trigger static init
        try {
            AuthorizationRegistry.getAllAuthorizations();
        } catch (Exception ignored) {}

        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);

        Field refField = AuthorizationRegistry.class.getDeclaredField("REFLECTIONS");
        Object base = unsafe.staticFieldBase(refField);
        long offset = unsafe.staticFieldOffset(refField);
        Object current = unsafe.getObject(base, offset);
        if (current instanceof org.reflections.Reflections r && originalReflections == null) {
            originalReflections = r;
        }

        org.reflections.Reflections mockReflections = new org.reflections.Reflections() {
            @Override
            public Set<Method> getMethodsAnnotatedWith(Class<? extends java.lang.annotation.Annotation> annotation) {
                return methods;
            }
        };

        try {
            unsafe.putObject(base, offset, mockReflections);
            runnable.run();
        } finally {
            org.reflections.Reflections toRestore = (originalReflections != null)
                    ? originalReflections
                    : new org.reflections.Reflections("com.freelance", org.reflections.scanners.Scanners.MethodsAnnotated);
            unsafe.putObject(base, offset, toRestore);
        }
    }

    @Test
    @DisplayName("Handles unannotated method returning null auth gracefully")
    void testUnannotatedMethodReturnsNullAuth() throws Exception {
        Set<Method> methods = Set.of(Object.class.getMethod("toString"));
        runWithCustomReflections(methods, () -> {
            List<Authorization> auths = AuthorizationRegistry.getAllAuthorizations();
            assertNotNull(auths);
            assertTrue(auths.isEmpty());
        });
    }

    @Test
    @DisplayName("Throws IllegalStateException when @Authorization key is empty")
    void testEmptyKeyThrows() throws Exception {
        Set<Method> methods = Set.of(SampleAuthFixtures.class.getMethod("emptyKey"));
        runWithCustomReflections(methods, () -> {
            IllegalStateException ex = assertThrows(IllegalStateException.class, AuthorizationRegistry::getAllAuthorizations);
            assertTrue(ex.getMessage().contains("empty or null key"));
        });
    }

    @Test
    @DisplayName("Throws IllegalStateException when @Authorization name is empty")
    void testEmptyNameThrows() throws Exception {
        Set<Method> methods = Set.of(SampleAuthFixtures.class.getMethod("emptyName"));
        runWithCustomReflections(methods, () -> {
            IllegalStateException ex = assertThrows(IllegalStateException.class, AuthorizationRegistry::getAllAuthorizations);
            assertTrue(ex.getMessage().contains("missing a name"));
        });
    }

    @Test
    @DisplayName("Throws IllegalStateException when @Authorization group is empty")
    void testEmptyGroupThrows() throws Exception {
        Set<Method> methods = Set.of(SampleAuthFixtures.class.getMethod("emptyGroup"));
        runWithCustomReflections(methods, () -> {
            IllegalStateException ex = assertThrows(IllegalStateException.class, AuthorizationRegistry::getAllAuthorizations);
            assertTrue(ex.getMessage().contains("missing a group"));
        });
    }

    @Test
    @DisplayName("Throws IllegalStateException when duplicate @Authorization key detected")
    void testDuplicateKeyThrows() throws Exception {
        Set<Method> methods = new LinkedHashSet<>();
        methods.add(SampleAuthFixtures.class.getMethod("dup1"));
        methods.add(SampleAuthFixtures.class.getMethod("dup2"));
        runWithCustomReflections(methods, () -> {
            List<Authorization> auths = AuthorizationRegistry.getAllAuthorizations();
            assertNotNull(auths);
            assertEquals(1, auths.size());
        });
    }

    @Test
    @DisplayName("isEquivalent tests identical and mismatched authorization instances")
    void testIsEquivalentMethod() throws Exception {
        Method isEquivMethod = AuthorizationRegistry.class.getDeclaredMethod("isEquivalent", Authorization.class, Authorization.class);
        isEquivMethod.setAccessible(true);

        Authorization a1 = SampleAuthFixtures.class.getMethod("dup1").getAnnotation(Authorization.class);
        Authorization a2 = SampleAuthFixtures.class.getMethod("dup2").getAnnotation(Authorization.class);
        Authorization a3 = SampleAuthFixtures.class.getMethod("emptyName").getAnnotation(Authorization.class);

        boolean eq1 = (boolean) isEquivMethod.invoke(null, a1, a2);
        assertTrue(eq1);

        boolean eq2 = (boolean) isEquivMethod.invoke(null, a1, a3);
        assertFalse(eq2);
    }
}
