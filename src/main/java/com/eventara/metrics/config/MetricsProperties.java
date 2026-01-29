package com.eventara.metrics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for distributed metrics.
 * Loaded from application.properties with prefix "eventara.metrics"
 */
@Configuration
@ConfigurationProperties(prefix = "eventara.metrics")
public class MetricsProperties {

    private Distributed distributed = new Distributed();
    private Bucket bucket = new Bucket();
    private Rollup rollup = new Rollup();

    public static class Distributed {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Bucket {
        private int sizeSeconds = 10;
        private int redisRetentionMinutes = 60; // Changed from hours to minutes
        private int timescaleRetentionDays = 30;

        public int getSizeSeconds() {
            return sizeSeconds;
        }

        public void setSizeSeconds(int sizeSeconds) {
            this.sizeSeconds = sizeSeconds;
        }

        public int getRedisRetentionMinutes() {
            return redisRetentionMinutes;
        }

        public void setRedisRetentionMinutes(int redisRetentionMinutes) {
            this.redisRetentionMinutes = redisRetentionMinutes;
        }

        public int getTimescaleRetentionDays() {
            return timescaleRetentionDays;
        }

        public void setTimescaleRetentionDays(int timescaleRetentionDays) {
            this.timescaleRetentionDays = timescaleRetentionDays;
        }
    }

    public static class Rollup {
        private int intervalSeconds = 60;

        public int getIntervalSeconds() {
            return intervalSeconds;
        }

        public void setIntervalSeconds(int intervalSeconds) {
            this.intervalSeconds = intervalSeconds;
        }
    }

    public Distributed getDistributed() {
        return distributed;
    }

    public void setDistributed(Distributed distributed) {
        this.distributed = distributed;
    }

    public Bucket getBucket() {
        return bucket;
    }

    public void setBucket(Bucket bucket) {
        this.bucket = bucket;
    }

    public Rollup getRollup() {
        return rollup;
    }

    public void setRollup(Rollup rollup) {
        this.rollup = rollup;
    }

    // Convenience methods
    public long getBucketSizeMs() {
        return bucket.sizeSeconds * 1000L;
    }

    public long getRedisRetentionMs() {
        return bucket.redisRetentionMinutes * 60 * 1000L;
    }
}
