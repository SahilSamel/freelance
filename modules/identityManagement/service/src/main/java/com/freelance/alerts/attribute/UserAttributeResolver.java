package com.freelance.alerts.attribute;

import com.freelance.user.entity.User;
import com.freelance.user.repository.UserRepository;

import java.util.*;

public class UserAttributeResolver implements IDomainAttributeResolver {

    private final UserRepository userRepository;

    public UserAttributeResolver() {
        this(new UserRepository());
    }

    public UserAttributeResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private static final Map<String, String> ATTRIBUTE_DESCRIPTIONS = Map.of(
            "USER.user_id", "Unique identifier (UUID) of the recipient user account",
            "USER.username", "Registered unique username/handle of the recipient user",
            "USER.first_name", "First name of the recipient user",
            "USER.name", "Display name of the recipient user"
    );

    @Override
    public String getDomain() {
        return "USER";
    }

    @Override
    public Set<String> getSupportedAttributes() {
        return ATTRIBUTE_DESCRIPTIONS.keySet();
    }

    @Override
    public String getAttributeDescription(String attributeKey) {
        return ATTRIBUTE_DESCRIPTIONS.get(attributeKey);
    }

    @Override
    public Map<String, String> resolve(UUID userId, Set<String> requestedAttributes) {
        if (userId == null || requestedAttributes == null || requestedAttributes.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();
        User user = userRepository.get(userId);

        for (String attr : requestedAttributes) {
            if (attr == null) continue;
            String normalized = attr.trim().toUpperCase();
            if (normalized.startsWith("USER.")) {
                String field = normalized.substring(5);
                if ("USERNAME".equals(field) || "FIRST_NAME".equals(field) || "FIRSTNAME".equals(field) || "NAME".equals(field)) {
                    if (user != null && user.getUsername() != null) {
                        String val = user.getUsername();
                        if ("FIRST_NAME".equals(field) || "FIRSTNAME".equals(field)) {
                            int sp = val.indexOf(' ');
                            if (sp > 0) {
                                val = val.substring(0, sp);
                            }
                        }
                        map.put(attr, val);
                    }
                } else if ("USER_ID".equals(field)) {
                    if (user != null && user.getId() != null) {
                        map.put(attr, user.getId().toString());
                    }
                }
            }
        }

        return map;
    }
}
