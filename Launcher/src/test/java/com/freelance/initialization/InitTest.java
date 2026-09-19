package com.freelance.initialization;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Init Bootstrap Unit Tests")
class InitTest {

    @Test
    @DisplayName("Should instantiate Init")
    void testInstantiation() {
        Init init = new Init();
        assertNotNull(init);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when EntityManagerFactory is null or closed")
    void testNullOrClosedEmfThrows() {
        IllegalStateException ex1 = assertThrows(IllegalStateException.class, () -> Init.startUp(null));
        assertTrue(ex1.getMessage().contains("EntityManagerFactory is null or closed"));

        EntityManagerFactory closedEmf = (EntityManagerFactory) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityManagerFactory.class},
                (proxy, method, args) -> {
                    if ("isOpen".equals(method.getName())) return false;
                    return null;
                }
        );

        IllegalStateException ex2 = assertThrows(IllegalStateException.class, () -> Init.startUp(closedEmf));
        assertTrue(ex2.getMessage().contains("EntityManagerFactory is null or closed"));
    }
}

