package com.freelance.alerts.service;

import com.freelance.alerts.attribute.IDomainAttributeResolver;
import com.freelance.alerts.dto.WhitelistedAttributeDTO;
import com.freelance.attribute.AttributeResolutionEngine;
import com.freelance.template.seeder.AlertsBootstrap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class AttributeResolutionEngineTest {

    @BeforeEach
    void setUp() {
        AttributeResolutionEngine.getInstance().resetForTesting();
        AlertsBootstrap.init();
    }

    @Test
    @DisplayName("Should list default whitelisted attributes with resolver-provided descriptions")
    void testDefaultWhitelistedAttributes() {
        AttributeResolutionEngine engine = AttributeResolutionEngine.getInstance();
        List<WhitelistedAttributeDTO> list = engine.listWhitelistedAttributes();

        assertNotNull(list);
        WhitelistedAttributeDTO userIdAttr = list.stream()
                .filter(a -> a.key().equals("USER.user_id"))
                .findFirst()
                .orElse(null);
        assertNotNull(userIdAttr);
        assertEquals("Unique identifier (UUID) of the recipient user account", userIdAttr.description());

        WhitelistedAttributeDTO usernameAttr = list.stream()
                .filter(a -> a.key().equals("USER.username"))
                .findFirst()
                .orElse(null);
        assertNotNull(usernameAttr);
        assertEquals("Registered unique username/handle of the recipient user", usernameAttr.description());
    }

    @Test
    @DisplayName("Should register custom domain resolver and resolve placeholders with custom descriptions")
    void testCustomDomainResolver() {
        AttributeResolutionEngine engine = AttributeResolutionEngine.getInstance();

        IDomainAttributeResolver customResolver = new IDomainAttributeResolver() {
            @Override
            public String getDomain() {
                return "PAYMENT";
            }

            @Override
            public Set<String> getSupportedAttributes() {
                return Set.of("PAYMENT.invoice_id", "PAYMENT.amount");
            }

            @Override
            public String getAttributeDescription(String attributeKey) {
                return switch (attributeKey) {
                    case "PAYMENT.invoice_id" -> "The invoice number for the transaction";
                    case "PAYMENT.amount" -> "The total monetary amount charged";
                    default -> null;
                };
            }

            @Override
            public Map<String, String> resolve(UUID userId, Set<String> requestedAttributes) {
                Map<String, String> result = new HashMap<>();
                if (requestedAttributes.contains("PAYMENT.invoice_id")) {
                    result.put("PAYMENT.invoice_id", "INV-9999");
                }
                if (requestedAttributes.contains("PAYMENT.amount")) {
                    result.put("PAYMENT.amount", "$150.00");
                }
                return result;
            }
        };

        engine.register(customResolver);

        List<WhitelistedAttributeDTO> list = engine.listWhitelistedAttributes();
        WhitelistedAttributeDTO paymentInvoice = list.stream()
                .filter(a -> a.key().equals("PAYMENT.invoice_id"))
                .findFirst()
                .orElse(null);
        assertNotNull(paymentInvoice);
        assertEquals("The invoice number for the transaction", paymentInvoice.description());

        UUID testUser = UUID.randomUUID();
        Map<String, String> resolved = engine.resolveAll(testUser, Set.of("PAYMENT.invoice_id", "PAYMENT.amount"));

        assertEquals("INV-9999", resolved.get("PAYMENT.invoice_id"));
        assertEquals("$150.00", resolved.get("PAYMENT.amount"));
    }

    @Test
    @DisplayName("Should extract domain correctly from dot separated attributes")
    void testDomainExtraction() {
        AttributeResolutionEngine engine = AttributeResolutionEngine.getInstance();

        IDomainAttributeResolver orderResolver = new IDomainAttributeResolver() {
            @Override
            public String getDomain() {
                return "ORDER";
            }

            @Override
            public Set<String> getSupportedAttributes() {
                return Set.of("ORDER.order_id", "ORDER.status");
            }

            @Override
            public String getAttributeDescription(String attributeKey) {
                return "Order details: " + attributeKey;
            }

            @Override
            public Map<String, String> resolve(UUID userId, Set<String> requestedAttributes) {
                Map<String, String> result = new HashMap<>();
                for (String attr : requestedAttributes) {
                    result.put(attr, "ORD-12345");
                }
                return result;
            }
        };

        engine.register(orderResolver);

        UUID testUser = UUID.randomUUID();
        Map<String, String> resolvedDot = engine.resolveAll(testUser, Set.of("ORDER.order_id", "ORDER.status"));
        assertEquals("ORD-12345", resolvedDot.get("ORDER.order_id"));
        assertEquals("ORD-12345", resolvedDot.get("ORDER.status"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when attribute key is null, blank, or has no delimiter")
    void testInvalidAttributeKeyThrowsException() {
        AttributeResolutionEngine engine = AttributeResolutionEngine.getInstance();
        UUID testUser = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> engine.resolveAll(testUser, Collections.singleton(null)));
        assertThrows(IllegalArgumentException.class, () -> engine.resolveAll(testUser, Set.of("   ")));
        assertThrows(IllegalArgumentException.class, () -> engine.resolveAll(testUser, Set.of("nodelimiter")));
        assertThrows(IllegalArgumentException.class, () -> engine.resolveAll(testUser, Set.of(".nodomain")));
        assertThrows(IllegalArgumentException.class, () -> engine.resolveAll(testUser, Set.of("nofield.")));
        assertThrows(IllegalArgumentException.class, () -> engine.resolveAll(testUser, Set.of("ORDER_order_id")));
    }
}