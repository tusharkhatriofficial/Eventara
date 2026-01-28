package com.eventara.metrics.model;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a single time bucket for metrics aggregation.
 * Contains aggregated counts, latency data, and breakdowns by source/type.
 */
public class MetricsBucket {

    private Instant bucketStart;
    private Instant bucketEnd;

    // Core counters
    private long totalEvents = 0;
    private long totalErrors = 0;

    // Latency aggregates
    private long latencySum = 0;
    private long latencyCount = 0;
    private Long latencyMin = null;
    private Long latencyMax = null;

    // Pre-computed percentiles (from sorted set)
    private Double latencyP50;
    private Double latencyP95;
    private Double latencyP99;

    // Breakdowns
    private Map<String, SourceMetrics> bySource = new ConcurrentHashMap<>();
    private Map<String, TypeMetrics> byEventType = new ConcurrentHashMap<>();
    private Map<String, Long> bySeverity = new ConcurrentHashMap<>();

    // Unique counts
    private int uniqueUsersEstimate = 0;
    private int uniqueSourcesEstimate = 0;
    private int uniqueEventTypesEstimate = 0;

    public MetricsBucket() {
    }

    public MetricsBucket(Instant bucketStart, Instant bucketEnd) {
        this.bucketStart = bucketStart;
        this.bucketEnd = bucketEnd;
    }

    /**
     * Metrics breakdown for a single source.
     */
    public static class SourceMetrics {
        private long events = 0;
        private long errors = 0;
        private long latencySum = 0;
        private long latencyCount = 0;

        public void incrementEvents() {
            events++;
        }

        public void incrementErrors() {
            errors++;
        }

        public void addLatency(long latency) {
            latencySum += latency;
            latencyCount++;
        }

        // Getters and setters
        public long getEvents() {
            return events;
        }

        public void setEvents(long events) {
            this.events = events;
        }

        public long getErrors() {
            return errors;
        }

        public void setErrors(long errors) {
            this.errors = errors;
        }

        public long getLatencySum() {
            return latencySum;
        }

        public void setLatencySum(long latencySum) {
            this.latencySum = latencySum;
        }

        public long getLatencyCount() {
            return latencyCount;
        }

        public void setLatencyCount(long latencyCount) {
            this.latencyCount = latencyCount;
        }

        public double getAvgLatency() {
            return latencyCount > 0 ? (double) latencySum / latencyCount : 0;
        }

        public double getErrorRate() {
            return events > 0 ? (errors * 100.0 / events) : 0;
        }
    }

    /**
     * Metrics breakdown for a single event type.
     */
    public static class TypeMetrics {
        private long count = 0;
        private long latencySum = 0;
        private long latencyCount = 0;

        public void incrementCount() {
            count++;
        }

        public void addLatency(long latency) {
            latencySum += latency;
            latencyCount++;
        }

        // Getters and setters
        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public long getLatencySum() {
            return latencySum;
        }

        public void setLatencySum(long latencySum) {
            this.latencySum = latencySum;
        }

        public long getLatencyCount() {
            return latencyCount;
        }

        public void setLatencyCount(long latencyCount) {
            this.latencyCount = latencyCount;
        }

        public double getAvgLatency() {
            return latencyCount > 0 ? (double) latencySum / latencyCount : 0;
        }
    }

    // Main getters and setters
    public Instant getBucketStart() {
        return bucketStart;
    }

    public void setBucketStart(Instant bucketStart) {
        this.bucketStart = bucketStart;
    }

    public Instant getBucketEnd() {
        return bucketEnd;
    }

    public void setBucketEnd(Instant bucketEnd) {
        this.bucketEnd = bucketEnd;
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(long totalEvents) {
        this.totalEvents = totalEvents;
    }

    public long getTotalErrors() {
        return totalErrors;
    }

    public void setTotalErrors(long totalErrors) {
        this.totalErrors = totalErrors;
    }

    public long getLatencySum() {
        return latencySum;
    }

    public void setLatencySum(long latencySum) {
        this.latencySum = latencySum;
    }

    public long getLatencyCount() {
        return latencyCount;
    }

    public void setLatencyCount(long latencyCount) {
        this.latencyCount = latencyCount;
    }

    public Long getLatencyMin() {
        return latencyMin;
    }

    public void setLatencyMin(Long latencyMin) {
        this.latencyMin = latencyMin;
    }

    public Long getLatencyMax() {
        return latencyMax;
    }

    public void setLatencyMax(Long latencyMax) {
        this.latencyMax = latencyMax;
    }

    public Double getLatencyP50() {
        return latencyP50;
    }

    public void setLatencyP50(Double latencyP50) {
        this.latencyP50 = latencyP50;
    }

    public Double getLatencyP95() {
        return latencyP95;
    }

    public void setLatencyP95(Double latencyP95) {
        this.latencyP95 = latencyP95;
    }

    public Double getLatencyP99() {
        return latencyP99;
    }

    public void setLatencyP99(Double latencyP99) {
        this.latencyP99 = latencyP99;
    }

    public Map<String, SourceMetrics> getBySource() {
        return bySource;
    }

    public void setBySource(Map<String, SourceMetrics> bySource) {
        this.bySource = bySource;
    }

    public Map<String, TypeMetrics> getByEventType() {
        return byEventType;
    }

    public void setByEventType(Map<String, TypeMetrics> byEventType) {
        this.byEventType = byEventType;
    }

    public Map<String, Long> getBySeverity() {
        return bySeverity;
    }

    public void setBySeverity(Map<String, Long> bySeverity) {
        this.bySeverity = bySeverity;
    }

    public int getUniqueUsersEstimate() {
        return uniqueUsersEstimate;
    }

    public void setUniqueUsersEstimate(int uniqueUsersEstimate) {
        this.uniqueUsersEstimate = uniqueUsersEstimate;
    }

    public int getUniqueSourcesEstimate() {
        return uniqueSourcesEstimate;
    }

    public void setUniqueSourcesEstimate(int uniqueSourcesEstimate) {
        this.uniqueSourcesEstimate = uniqueSourcesEstimate;
    }

    public int getUniqueEventTypesEstimate() {
        return uniqueEventTypesEstimate;
    }

    public void setUniqueEventTypesEstimate(int uniqueEventTypesEstimate) {
        this.uniqueEventTypesEstimate = uniqueEventTypesEstimate;
    }

    // Convenience methods
    public double getAvgLatency() {
        return latencyCount > 0 ? (double) latencySum / latencyCount : 0;
    }

    public double getErrorRate() {
        return totalEvents > 0 ? (totalErrors * 100.0 / totalEvents) : 0;
    }
}
