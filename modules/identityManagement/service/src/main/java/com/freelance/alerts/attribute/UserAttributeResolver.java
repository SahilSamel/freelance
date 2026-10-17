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

        User user = userRepository.get(userId);
        if (user == null) {
            return Collections.emptyMap();
        }

        String firstName = resolveFirstName(user);
        String fullName = resolveFullName(user);

        Map<String, String> map = new HashMap<>();
        for (String attr : requestedAttributes) {
            if (attr == null) continue;
            String normalized = attr.trim().toUpperCase();
            switch (normalized) {
                case "USER.FIRST_NAME", "USER.FIRSTNAME" -> map.put(attr, firstName);
                case "USER.NAME" -> map.put(attr, fullName);
                default -> {}
            }
        }

        return map;
    }

    private String resolveFirstName(User user) {
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            return user.getFirstName().trim();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank() && !user.getUsername().startsWith("mem_")) {
            String u = user.getUsername().trim();
            int sp = u.indexOf(' ');
            return sp > 0 ? u.substring(0, sp) : u;
        }
        return "Member";
    }

    private String resolveFullName(User user) {
        String fn = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String ln = user.getLastName() != null ? user.getLastName().trim() : "";
        if (!fn.isEmpty() && !ln.isEmpty()) {
            return fn + " " + ln;
        }
        if (!fn.isEmpty()) {
            return fn;
        }
        if (!ln.isEmpty()) {
            return ln;
        }
        if (user.getUsername() != null && !user.getUsername().isBlank() && !user.getUsername().startsWith("mem_")) {
            return user.getUsername().trim();
        }
        return "Member";
    }
}
