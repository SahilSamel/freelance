package com.freelance.event.limiter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketRateLimiterTest {

    @Test
    @DisplayName("Acquire throttles requests and blocks when tokens are unavailable")
    void testThrottledAcquisition() throws InterruptedException {
        // Rate: 10 tokens per second (1 token every 100ms)
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(10.0, 1.0);

        // Exhaust initial 1 token
        limiter.acquire();

        long startTime = System.currentTimeMillis();

        // Next 3 acquisitions must wait ~100ms each (total >= 250ms)
        for (int i = 0; i < 3; i++) {
            limiter.acquire();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed >= 250, "Expected at least 250ms of throttling, elapsed: " + elapsed + "ms");
    }

    @Test
    @DisplayName("Rate limiter with 0 or negative rate is unlimited and never blocks")
    void testUnlimitedRate() throws InterruptedException {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(0.0);

        for (int i = 0; i < 100; i++) {
            limiter.acquire();
        }
    }

    @Test
    @DisplayName("Dynamically updating rate limit modifies token acquisition throughput immediately")
    void testDynamicRateUpdate() throws InterruptedException {
        // Start throttled at 2 tokens/sec
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(2.0, 1.0);
        assertEquals(2.0, limiter.getRatePerSecond());

        // Update dynamically to unlimited (0.0)
        limiter.setRatePerSecond(0.0);
        assertEquals(0.0, limiter.getRatePerSecond());

        // Acquires should now be instantaneous
        for (int i = 0; i < 50; i++) {
            limiter.acquire();
        }
    }
}
