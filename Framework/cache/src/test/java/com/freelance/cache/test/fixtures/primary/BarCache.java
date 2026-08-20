package com.freelance.cache.test.fixtures.primary;

import com.freelance.config.AbstractCacheConfig;

/**
 * Test fixture: String→String cache with a 1-second TTL.
 * Used to verify TTL expiry behaviour without waiting for the 5-minute background sweep.
 */
public class BarCache extends AbstractCacheConfig<String, String> {

    @Override
    protected String name() {
        return "BarCache";
    }

    @Override
    protected long ttlSeconds() {
        return 1L;
    }

    @Override
    protected String load(String key) {
        return "bar-value-" + key;
    }
}
