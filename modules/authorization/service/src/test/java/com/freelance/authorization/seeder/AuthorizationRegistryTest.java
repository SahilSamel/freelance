package com.freelance.authorization.seeder;

import com.freelance.rest.app.endpoint.annotation.Authorization;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationRegistryTest {

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
}
