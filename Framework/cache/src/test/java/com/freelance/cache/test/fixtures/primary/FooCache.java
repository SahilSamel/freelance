package com.freelance.cache.test.fixtures.primary;

import com.freelance.config.AbstractCacheConfig;

/**
 * Test fixture: simple String→String cache with no TTL.
 * load() returns a deterministic value so tests can assert on it.
 */
public class FooCache extends AbstractCacheConfig<String, String> {

    @Override
    protected String name() {
        return "FooCache";
    }

    @Override
    protected String load(String key) {
        return "foo-value-" + key;
    }
}
