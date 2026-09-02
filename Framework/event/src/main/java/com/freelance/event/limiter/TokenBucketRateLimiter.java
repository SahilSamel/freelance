package com.freelance.event.limiter;

import java.util.concurrent.TimeUnit;

/**
 * Thread-safe Token Bucket rate limiter enforcing steady task execution throughput per topic.
 * <p>
 * <b>How Token Bucket &amp; {@code refill()} work:</b>
 * <ul>
 *   <li>The limiter models a virtual bucket with capacity {@code maxTokens}, replenished at {@code ratePerSecond}.</li>
 *   <li>Every task consumed by a worker requires 1 token.</li>
 *   <li>Instead of running a background timer thread to continuously add tokens, {@link #refill()} uses
 *       <b>lazy evaluation</b>: whenever {@link #acquire()} is called, it calculates how much time has passed
 *       since the last call ({@code elapsedNanos = now - lastRefillNanos}) and adds the proportionally earned tokens
 *       ({@code (elapsedNanos * ratePerSecond) / 1e9}) into {@code availableTokens} up to {@code maxTokens}.</li>
 *   <li>If {@code availableTokens >= 1.0}, a token is deducted and the worker proceeds immediately.</li>
 *   <li>If {@code availableTokens < 1.0}, it computes the exact remaining nanoseconds until 1 full token is available
 *       and puts the calling worker thread to sleep for that duration.</li>
 * </ul>
 * </p>
 */
public class TokenBucketRateLimiter {

    private final double ratePerSecond;
    private final double maxTokens;
    private double availableTokens;
    private long lastRefillNanos;

    public TokenBucketRateLimiter(double ratePerSecond) {
        this(ratePerSecond, Math.max(1.0, ratePerSecond));
    }

    public TokenBucketRateLimiter(double ratePerSecond, double maxBurst) {
        this.ratePerSecond = ratePerSecond;
        this.maxTokens = Math.max(1.0, maxBurst);
        this.availableTokens = this.maxTokens;
        this.lastRefillNanos = System.nanoTime();
    }

    /**
     * Acquires 1 token from the bucket, blocking the calling thread until a token becomes available.
     * <p>
     * If rate limiting is disabled ({@code ratePerSecond <= 0.0}), this method returns immediately without blocking.
     * </p>
     *
     * @throws InterruptedException if the thread is interrupted while sleeping for tokens
     */
    public synchronized void acquire() throws InterruptedException {
        if (ratePerSecond <= 0.0) {
            return;
        }

        while (true) {
            refill();

            if (availableTokens >= 1.0) {
                availableTokens -= 1.0;
                return;
            }

            double missing = 1.0 - availableTokens;
            long waitNanos = (long) ((missing / ratePerSecond) * 1_000_000_000L);
            if (waitNanos > 0) {
                long millis = TimeUnit.NANOSECONDS.toMillis(waitNanos);
                int nanos = (int) (waitNanos % 1_000_000L);
                if (millis > 0 || nanos > 0) {
                    Thread.sleep(Math.max(1L, millis), nanos);
                }
            }
        }
    }

    /**
     * Lazily replenishes available tokens based on elapsed nanoseconds since last invocation.
     */
    private void refill() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastRefillNanos;
        if (elapsedNanos > 0) {
            double newTokens = (elapsedNanos * ratePerSecond) / 1_000_000_000.0;
            availableTokens = Math.min(maxTokens, availableTokens + newTokens);
            lastRefillNanos = now;
        }
    }
}
