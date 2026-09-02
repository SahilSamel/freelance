package com.freelance.event.consumer;

/**
 * Internal configuration record holding worker thread concurrency, throughput throttling,
 * and exponential backoff retry policies for a consumer topic.
 * <p>
 * During standard application execution, values in this record are automatically extracted
 * and mapped from the developer-facing {@link com.freelance.event.model.EventConsumer} annotation
 * by the framework scanner. This record and its builder are also used directly for programmatic
 * registration and standalone unit testing.
 * </p>
 * @see com.freelance.event.model.EventConsumer
 */
public record ConsumerConfig(
        int concurrency,
        double rateLimitPerSecond,
        int maxRetries,
        long backoffMillis,
        double backoffMultiplier
) {
    public ConsumerConfig {
        concurrency = Math.max(1, concurrency);
        rateLimitPerSecond = Math.max(0.0, rateLimitPerSecond);
        maxRetries = Math.max(0, maxRetries);
        backoffMillis = Math.max(0L, backoffMillis);
        backoffMultiplier = backoffMultiplier <= 0 ? 1.0 : backoffMultiplier;
    }

    public static ConsumerConfig defaultForTopic() {
        return new ConsumerConfig(1, 0.0, 3, 1000L, 2.0);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int concurrency = 1;
        private double rateLimitPerSecond = 0.0;
        private int maxRetries = 3;
        private long backoffMillis = 1000L;
        private double backoffMultiplier = 2.0;

        public Builder concurrency(int concurrency) {
            this.concurrency = concurrency;
            return this;
        }

        public Builder rateLimitPerSecond(double rateLimitPerSecond) {
            this.rateLimitPerSecond = rateLimitPerSecond;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder backoffMillis(long backoffMillis) {
            this.backoffMillis = backoffMillis;
            return this;
        }

        public Builder backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        public ConsumerConfig build() {
            return new ConsumerConfig(concurrency, rateLimitPerSecond, maxRetries, backoffMillis, backoffMultiplier);
        }
    }
}
