package com.freelance.tenant.control.key;

import com.freelance.tenant.fixtures.TestContextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlatformVendorKeyService Unit Tests")
class PlatformVendorKeyServiceTest {

    private PlatformVendorKeyService keyService;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
        keyService = new PlatformVendorKeyService();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Default constructor instantiates successfully")
    void testInstantiation() {
        assertNotNull(keyService);
    }
}
