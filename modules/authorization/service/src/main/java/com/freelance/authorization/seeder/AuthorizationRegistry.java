package com.freelance.authorization.seeder;

import com.freelance.rest.app.endpoint.annotation.Authorization;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Method;
import java.util.*;

public final class AuthorizationRegistry {

    private static final String BASE_PACKAGE = "com.freelance";
    private static final Reflections REFLECTIONS = new Reflections(BASE_PACKAGE, Scanners.MethodsAnnotated);

    private AuthorizationRegistry() {
    }

    public static List<Authorization> getAllAuthorizations() {
        Set<Method> annotatedMethods = REFLECTIONS.getMethodsAnnotatedWith(Authorization.class);
        Map<String, Authorization> uniqueAuthMap = new LinkedHashMap<>();

        for (Method method : annotatedMethods) {
            Authorization auth = method.getAnnotation(Authorization.class);
            if (auth == null) {
                continue;
            }

            String key = auth.key();
            if (key == null || key.isBlank()) {
                throw new IllegalStateException("FATAL: Method " + method + " has @Authorization with an empty or null key!");
            }

            if (auth.name() == null || auth.name().isBlank()) {
                throw new IllegalStateException("FATAL: Authorization key '" + key + "' on " + method + " is missing a name!");
            }

            if (auth.group() == null || auth.group().isBlank()) {
                throw new IllegalStateException("FATAL: Authorization key '" + key + "' on " + method + " is missing a group!");
            }

            Authorization existing = uniqueAuthMap.put(key, auth);
            if (existing != null && !isEquivalent(existing, auth)) {
                throw new IllegalStateException(String.format(
                        "FATAL: Duplicate authorization key '%s' detected with conflicting definitions across methods [%s] and [%s]!",
                        key, existing, auth
                ));
            }
        }

        return new ArrayList<>(uniqueAuthMap.values());
    }

    private static boolean isEquivalent(Authorization a1, Authorization a2) {
        return Objects.equals(a1.key(), a2.key());
    }
}
