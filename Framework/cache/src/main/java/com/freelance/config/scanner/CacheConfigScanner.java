package com.freelance.config.scanner;

import com.freelance.config.AbstractCacheConfig;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Scans the classpath for concrete AbstractCacheConfig subclasses so
 * cache designers don't need to register their caches anywhere manually.
 */
public final class CacheConfigScanner {

    private static final String CLASS_NAME = CacheConfigScanner.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(CacheConfigScanner.class);

    @SuppressWarnings("unchecked")
    public Set<Class<? extends AbstractCacheConfig<?, ?>>> scan(String basePackage) {
        Reflections reflections = new Reflections(basePackage);

        Set<?> found = reflections.getSubTypesOf(AbstractCacheConfig.class);

        if (found.isEmpty()) {
            logger.info("[{}] No cache configs found in package '{}'.", CLASS_NAME, basePackage);
        }

        return (Set<Class<? extends AbstractCacheConfig<?, ?>>>) found;
    }
}