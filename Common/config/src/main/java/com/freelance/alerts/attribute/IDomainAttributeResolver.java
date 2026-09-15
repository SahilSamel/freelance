package com.freelance.alerts.attribute;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Extension contract implemented by domain modules to dynamically resolve user and entity placeholder
 * attributes (e.g., {@code {{USER.username}}}, {@code {{PAYMENT.amount}}}) referenced in alert templates.
 * <p>
 * <b>Mandated Package Convention:</b><br/>
 * All implementations of {@link IDomainAttributeResolver} MUST reside under package
 * {@code com.freelance.alerts.attribute} (or its subpackages) to be discovered and registered
 * automatically via classpath reflection scanning during application startup.
 * </p>
 */
public interface IDomainAttributeResolver {

    /**
     * Unique domain identifier for this resolver (e.g., {@code "USER"}, {@code "PAYMENT"}, {@code "PROJECT"}).
     * <p>
     * Placeholders prefixed with {@code "{{DOMAIN.*}}"} are routed to the matching domain resolver.
     * </p>
     *
     * @return uppercase domain name string
     */
    String getDomain();

    /**
     * Complete set of dot-separated attribute keys supported by this resolver.
     * <p>
     * <b>Example:</b> {@code Set.of("USER.user_id", "USER.username", "USER.phone", "USER.email")}
     * </p>
     *
     * @return non-null set of supported attribute keys
     */
    Set<String> getSupportedAttributes();

    /**
     * Returns a human-readable description explaining what the given attribute placeholder represents.
     * <p>
     * Implementations should provide clear explanations to communicate its purpose and usage
     * to developers and users configuring alert templates.
     * </p>
     *
     * @param attributeKey the attribute placeholder key (e.g., {@code "USER.username"})
     * @return human-readable description of the attribute
     */
    String getAttributeDescription(String attributeKey);

    /**
     * Resolves the requested attribute placeholders for the target user.
     *
     * @param userId              target recipient user ID
     * @param requestedAttributes subset of supported attributes present in the template to be resolved
     * @return key-value map of resolved placeholder strings (e.g., {@code "USER.username" -> "john_doe"})
     */
    Map<String, String> resolve(UUID userId, Set<String> requestedAttributes);
}
