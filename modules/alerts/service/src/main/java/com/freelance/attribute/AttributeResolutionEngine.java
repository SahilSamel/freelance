package com.freelance.attribute;

import com.freelance.alerts.attribute.IDomainAttributeResolver;
import com.freelance.alerts.dto.WhitelistedAttributeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AttributeResolutionEngine {

    private static final Logger logger = LoggerFactory.getLogger(AttributeResolutionEngine.class);

    private static final class Holder {
        private static final AttributeResolutionEngine INSTANCE = new AttributeResolutionEngine();
    }

    public static AttributeResolutionEngine getInstance() {
        return Holder.INSTANCE;
    }

    private final Map<String, IDomainAttributeResolver> resolvers = new ConcurrentHashMap<>();

    private AttributeResolutionEngine() {
    }

    public void register(IDomainAttributeResolver resolver) {
        if (resolver != null && resolver.getDomain() != null) {
            resolvers.put(resolver.getDomain().toUpperCase(), resolver);
            logger.info("[AttributeResolutionEngine] Registered attribute resolver for domain: {}", resolver.getDomain());
        }
    }

    public void resetForTesting() {
        resolvers.clear();
    }

    public List<WhitelistedAttributeDTO> listWhitelistedAttributes() {
        List<WhitelistedAttributeDTO> list = new ArrayList<>();
        resolvers.values().forEach(r -> {
            for (String attr : r.getSupportedAttributes()) {
                list.add(new WhitelistedAttributeDTO(attr, r.getDomain(), r.getAttributeDescription(attr)));
            }
        });
        return list;
    }

    public Map<String, String> resolveAll(UUID userId, Set<String> requestedAttributes) {
        if (userId == null || requestedAttributes == null || requestedAttributes.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> resolved = new HashMap<>();
        Map<String, Set<String>> grouped = new HashMap<>();

        for (String attr : requestedAttributes) {
            String domain = extractDomain(attr);
            grouped.computeIfAbsent(domain.toUpperCase(), k -> new HashSet<>()).add(attr);
        }

        for (Map.Entry<String, Set<String>> entry : grouped.entrySet()) {
            IDomainAttributeResolver resolver = resolvers.get(entry.getKey());
            if (resolver != null) {
                Map<String, String> values = resolver.resolve(userId, entry.getValue());
                if (values != null) {
                    resolved.putAll(values);
                }
            } else {
                logger.warn("[AttributeResolutionEngine] No resolver registered for domain: {}", entry.getKey());
            }
        }

        return resolved;
    }

    private String extractDomain(String attributeKey) {
        if (attributeKey == null || attributeKey.isBlank()) {
            throw new IllegalArgumentException("Attribute key cannot be null or blank");
        }
        int delimiter = attributeKey.indexOf('.');

        if (delimiter <= 0 || delimiter == attributeKey.length() - 1) {
            throw new IllegalArgumentException(
                    "Invalid attribute key format: '" + attributeKey + "'. Expected format: <DOMAIN>.<attribute> (e.g., USER.username)"
            );
        }
        return attributeKey.substring(0, delimiter);
    }
}