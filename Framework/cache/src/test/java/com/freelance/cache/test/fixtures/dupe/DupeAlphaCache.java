package com.freelance.cache.test.fixtures.dupe;

import com.freelance.config.AbstractCacheConfig;

/**
 * Test fixture: intentionally shares the same cache name as {@link DupeBetaCache}
 * so that CacheManager.init() throws an IllegalStateException on duplicate detection.
 */
public class DupeAlphaCache extends AbstractCacheConfig<String, String> {

    @Override
    protected String name() {
        return "DuplicateCache";
    }

    @Override
    protected String load(String key) {
        return "alpha-" + key;
    }
}
