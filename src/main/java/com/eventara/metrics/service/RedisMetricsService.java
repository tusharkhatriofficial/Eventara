package com.eventara.metrics.service;

import com.eventara.common.dto.EventDto;
import com.eventara.metrics.config.MetricsProperties;
import com.eventara.metrics.model.MetricsBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based metrics storage for real-time data.
 * Uses time-bucketed keys with TTL for automatic expiry.
 * 
 * Key structure:
 * - metrics:bucket:{bucketTimestamp}:events - total event count
 * - metrics:bucket:{bucketTimestamp}:errors - total error count
 * - metrics:bucket:{bucketTimestamp}:latency - latency sum and count
 * - metrics:bucket:{bucketTimestamp}:source:{name} - per-source metrics
 * - metrics:bucket:{bucketTimestamp}:type:{name} - per-type metrics
 * - metrics:latencies:{bucketTimestamp} - sorted set for percentiles
 */
@Service
public class RedisMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(RedisMetricsService.class);

    private static final String BUCKET_PREFIX = "metrics:bucket:";
    private static final String LATENCIES_PREFIX = "metrics:latencies:";
    private static final String FIELD_EVENTS = "events";
    private static final String FIELD_ERRORS = "errors";
    private static final String FIELD_LATENCY_SUM = "latency_sum";
    private static final String FIELD_LATENCY_COUNT = "latency_count";
    private static final String FIELD_LATENCY_MIN = "latency_min";
    private static final String FIELD_LATENCY_MAX = "latency_max";

    @Autowired
    private RedisTemplate<String, String> stringRedisTemplate;

    @Autowired
    private MetricsProperties metricsProperties;

    /**
     * Record an event in the current time bucket.
     * Uses atomic Redis operations for thread-safety across instances.
     */
    public void recordEvent(EventDto event) {
        try {
            long now = System.currentTimeMillis();
            long bucketStart = getBucketStart(now);
            String bucketKey = BUCKET_PREFIX + bucketStart;

            long ttlSeconds = metricsProperties.getBucket().getRedisRetentionMinutes() * 60;

            // Atomic increment for total events
            stringRedisTemplate.opsForHash().increment(bucketKey, FIELD_EVENTS, 1);

            // Track errors
            if (event.isError()) {
                stringRedisTemplate.opsForHash().increment(bucketKey, FIELD_ERRORS, 1);
            }

            // Track latency
            long latency = event.getProcessingLatencyMs();
            if (latency > 0) {
                stringRedisTemplate.opsForHash().increment(bucketKey, FIELD_LATENCY_SUM, latency);
                stringRedisTemplate.opsForHash().increment(bucketKey, FIELD_LATENCY_COUNT, 1);

                // Update min/max
                updateMinMax(bucketKey, latency);

                // Add to sorted set for percentile calculation
                String latencyKey = LATENCIES_PREFIX + bucketStart;
                stringRedisTemplate.opsForZSet().add(latencyKey, String.valueOf(latency), latency);
                stringRedisTemplate.expire(latencyKey, ttlSeconds, TimeUnit.SECONDS);
            }

            // Track by source
            if (event.getSource() != null) {
                String sourceKey = bucketKey + ":source:" + event.getSource();
                stringRedisTemplate.opsForHash().increment(sourceKey, FIELD_EVENTS, 1);
                if (event.isError()) {
                    stringRedisTemplate.opsForHash().increment(sourceKey, FIELD_ERRORS, 1);
                }
                if (latency > 0) {
                    stringRedisTemplate.opsForHash().increment(sourceKey, FIELD_LATENCY_SUM, latency);
                    stringRedisTemplate.opsForHash().increment(sourceKey, FIELD_LATENCY_COUNT, 1);
                }
                stringRedisTemplate.expire(sourceKey, ttlSeconds, TimeUnit.SECONDS);
            }

            // Track by event type
            if (event.getEventType() != null) {
                String typeKey = bucketKey + ":type:" + event.getEventType();
                stringRedisTemplate.opsForHash().increment(typeKey, FIELD_EVENTS, 1);
                if (latency > 0) {
                    stringRedisTemplate.opsForHash().increment(typeKey, FIELD_LATENCY_SUM, latency);
                    stringRedisTemplate.opsForHash().increment(typeKey, FIELD_LATENCY_COUNT, 1);
                }
                stringRedisTemplate.expire(typeKey, ttlSeconds, TimeUnit.SECONDS);
            }

            // Track by severity
            if (event.getSeverity() != null) {
                String severityKey = bucketKey + ":severity";
                stringRedisTemplate.opsForHash().increment(severityKey, event.getSeverity(), 1);
                stringRedisTemplate.expire(severityKey, ttlSeconds, TimeUnit.SECONDS);
            }

            // Set TTL on main bucket
            stringRedisTemplate.expire(bucketKey, ttlSeconds, TimeUnit.SECONDS);

            logger.debug("Recorded event in bucket {}: type={}, source={}",
                    bucketStart, event.getEventType(), event.getSource());

        } catch (Exception e) {
            logger.error("Failed to record event to Redis: {}", e.getMessage(), e);
        }
    }

    /**
     * Get aggregated metrics for a time window.
     */
    public MetricsBucket getMetrics(Duration window) {
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();

        return aggregateBuckets(windowStart, now);
    }

    /**
     * Get metrics for the last N minutes.
     */
    public MetricsBucket getMetricsLastMinutes(int minutes) {
        return getMetrics(Duration.ofMinutes(minutes));
    }

    /**
     * Get metrics for the PREVIOUS window (before the current window).
     * Used for rate of change detection.
     * 
     * Example: If window is 5 minutes, this returns metrics from 10-5 minutes ago.
     * 
     * @param windowMinutes Size of each window in minutes
     * @return MetricsBucket for the previous window
     */
    public MetricsBucket getMetricsPreviousWindow(int windowMinutes) {
        long now = System.currentTimeMillis();
        long windowMs = windowMinutes * 60 * 1000L;

        // Previous window: from (2*window ago) to (1*window ago)
        long prevWindowEnd = now - windowMs;
        long prevWindowStart = now - (2 * windowMs);

        return aggregateBuckets(prevWindowStart, prevWindowEnd);
    }

    /**
     * Get metrics for a specific source in the PREVIOUS window.
     */
    public MetricsBucket getMetricsForSourcePreviousWindow(String source, int windowMinutes) {
        if (source == null || source.isEmpty()) {
            return getMetricsPreviousWindow(windowMinutes);
        }

        long now = System.currentTimeMillis();
        long windowMs = windowMinutes * 60 * 1000L;
        long prevWindowEnd = now - windowMs;
        long prevWindowStart = now - (2 * windowMs);

        return aggregateBucketsForSource(prevWindowStart, prevWindowEnd, source);
    }

    /**
     * Get aggregated metrics for a SPECIFIC SOURCE in the last N minutes.
     * This enables source-specific threshold rule evaluation.
     * 
     * @param source  The source name to filter by
     * @param minutes Time window in minutes
     * @return MetricsBucket with source-specific metrics (totalEvents/Errors from
     *         that source)
     */
    public MetricsBucket getMetricsForSource(String source, int minutes) {
        if (source == null || source.isEmpty()) {
            return getMetricsLastMinutes(minutes);
        }

        long now = System.currentTimeMillis();
        long windowStart = now - (minutes * 60 * 1000L);

        return aggregateBucketsForSource(windowStart, now, source);
    }

    /**
     * Get aggregated metrics for a SPECIFIC EVENT TYPE in the last N minutes.
     * 
     * @param eventType The event type to filter by
     * @param minutes   Time window in minutes
     * @return MetricsBucket with type-specific metrics
     */
    public MetricsBucket getMetricsForEventType(String eventType, int minutes) {
        if (eventType == null || eventType.isEmpty()) {
            return getMetricsLastMinutes(minutes);
        }

        long now = System.currentTimeMillis();
        long windowStart = now - (minutes * 60 * 1000L);

        return aggregateBucketsForEventType(windowStart, now, eventType);
    }

    /**
     * Get aggregated metrics for MULTIPLE EVENT TYPES in the last N minutes.
     * Combines metrics from all specified event types.
     *
     * @param eventTypes List of event types to include
     * @param minutes    Time window in minutes
     * @return MetricsBucket with combined type metrics
     */
    public MetricsBucket getMetricsForEventTypes(List<String> eventTypes, int minutes) {
        if (eventTypes == null || eventTypes.isEmpty()) {
            return getMetricsLastMinutes(minutes);
        }

        List<String> filteredTypes = eventTypes.stream()
                .filter(Objects::nonNull)
                .filter(t -> !t.isBlank())
                .toList();

        if (filteredTypes.isEmpty()) {
            return getMetricsLastMinutes(minutes);
        }

        if (filteredTypes.size() == 1) {
            return getMetricsForEventType(filteredTypes.get(0), minutes);
        }

        long now = System.currentTimeMillis();
        long windowStart = now - (minutes * 60 * 1000L);

        MetricsBucket combined = new MetricsBucket(
                Instant.ofEpochMilli(windowStart),
                Instant.ofEpochMilli(now));

        for (String eventType : filteredTypes) {
            MetricsBucket typeBucket = aggregateBucketsForEventType(windowStart, now, eventType);
            combined.setTotalEvents(combined.getTotalEvents() + typeBucket.getTotalEvents());
            combined.setTotalErrors(combined.getTotalErrors() + typeBucket.getTotalErrors());
            combined.setLatencySum(combined.getLatencySum() + typeBucket.getLatencySum());
            combined.setLatencyCount(combined.getLatencyCount() + typeBucket.getLatencyCount());

            if (typeBucket.getLatencyMin() != null) {
                if (combined.getLatencyMin() == null || typeBucket.getLatencyMin() < combined.getLatencyMin()) {
                    combined.setLatencyMin(typeBucket.getLatencyMin());
                }
            }
            if (typeBucket.getLatencyMax() != null) {
                if (combined.getLatencyMax() == null || typeBucket.getLatencyMax() > combined.getLatencyMax()) {
                    combined.setLatencyMax(typeBucket.getLatencyMax());
                }
            }
        }

        return combined;
    }

    /**
     * Get aggregated metrics for MULTIPLE SOURCES in the last N minutes.
     * Combines metrics from all specified sources.
     * 
     * @param sources List of source names to include
     * @param minutes Time window in minutes
     * @return MetricsBucket with combined source metrics
     */
    public MetricsBucket getMetricsForSources(List<String> sources, int minutes) {
        if (sources == null || sources.isEmpty()) {
            return getMetricsLastMinutes(minutes);
        }

        if (sources.size() == 1) {
            return getMetricsForSource(sources.get(0), minutes);
        }

        // Combine metrics from multiple sources
        MetricsBucket combined = new MetricsBucket(
                Instant.now().minusSeconds(minutes * 60L),
                Instant.now());

        for (String source : sources) {
            MetricsBucket sourceBucket = getMetricsForSource(source, minutes);
            combined.setTotalEvents(combined.getTotalEvents() + sourceBucket.getTotalEvents());
            combined.setTotalErrors(combined.getTotalErrors() + sourceBucket.getTotalErrors());
            combined.setLatencySum(combined.getLatencySum() + sourceBucket.getLatencySum());
            combined.setLatencyCount(combined.getLatencyCount() + sourceBucket.getLatencyCount());

            // Track min/max
            if (sourceBucket.getLatencyMin() != null) {
                if (combined.getLatencyMin() == null || sourceBucket.getLatencyMin() < combined.getLatencyMin()) {
                    combined.setLatencyMin(sourceBucket.getLatencyMin());
                }
            }
            if (sourceBucket.getLatencyMax() != null) {
                if (combined.getLatencyMax() == null || sourceBucket.getLatencyMax() > combined.getLatencyMax()) {
                    combined.setLatencyMax(sourceBucket.getLatencyMax());
                }
            }
        }

        return combined;
    }

    /**
     * Get all buckets in a time range (for rollup to TimescaleDB).
     */
    public List<MetricsBucket> getBuckets(Instant start, Instant end) {
        List<MetricsBucket> buckets = new ArrayList<>();
        long bucketSizeMs = metricsProperties.getBucketSizeMs();

        for (long t = start.toEpochMilli(); t < end.toEpochMilli(); t += bucketSizeMs) {
            MetricsBucket bucket = getBucket(t);
            if (bucket.getTotalEvents() > 0) {
                buckets.add(bucket);
            }
        }

        return buckets;
    }

    /**
     * Get a single bucket by its start timestamp.
     */
    private MetricsBucket getBucket(long bucketStart) {
        String bucketKey = BUCKET_PREFIX + bucketStart;
        long bucketSizeMs = metricsProperties.getBucketSizeMs();

        MetricsBucket bucket = new MetricsBucket(
                Instant.ofEpochMilli(bucketStart),
                Instant.ofEpochMilli(bucketStart + bucketSizeMs));

        // Get core counters
        bucket.setTotalEvents(getLongFromHash(bucketKey, FIELD_EVENTS));
        bucket.setTotalErrors(getLongFromHash(bucketKey, FIELD_ERRORS));
        bucket.setLatencySum(getLongFromHash(bucketKey, FIELD_LATENCY_SUM));
        bucket.setLatencyCount(getLongFromHash(bucketKey, FIELD_LATENCY_COUNT));
        bucket.setLatencyMin(getLongFromHashOrNull(bucketKey, FIELD_LATENCY_MIN));
        bucket.setLatencyMax(getLongFromHashOrNull(bucketKey, FIELD_LATENCY_MAX));

        // Calculate percentiles from sorted set
        String latencyKey = LATENCIES_PREFIX + bucketStart;
        Long size = stringRedisTemplate.opsForZSet().size(latencyKey);
        if (size != null && size > 0) {
            bucket.setLatencyP50(getPercentile(latencyKey, size, 0.50));
            bucket.setLatencyP95(getPercentile(latencyKey, size, 0.95));
            bucket.setLatencyP99(getPercentile(latencyKey, size, 0.99));
        }

        // Load per-source metrics
        loadSourceMetrics(bucketKey, bucket);

        // Load per-type metrics
        loadTypeMetrics(bucketKey, bucket);

        // Load severity breakdown
        loadSeverityMetrics(bucketKey, bucket);

        return bucket;
    }

    /**
     * Aggregate all buckets in a time range.
     */
    private MetricsBucket aggregateBuckets(long startMs, long endMs) {
        MetricsBucket result = new MetricsBucket(
                Instant.ofEpochMilli(startMs),
                Instant.ofEpochMilli(endMs));

        long bucketSizeMs = metricsProperties.getBucketSizeMs();
        long bucketStart = getBucketStart(startMs);

        List<Long> allLatencies = new ArrayList<>();

        for (long t = bucketStart; t <= endMs; t += bucketSizeMs) {
            MetricsBucket bucket = getBucket(t);

            result.setTotalEvents(result.getTotalEvents() + bucket.getTotalEvents());
            result.setTotalErrors(result.getTotalErrors() + bucket.getTotalErrors());
            result.setLatencySum(result.getLatencySum() + bucket.getLatencySum());
            result.setLatencyCount(result.getLatencyCount() + bucket.getLatencyCount());

            // Track min/max
            if (bucket.getLatencyMin() != null) {
                if (result.getLatencyMin() == null || bucket.getLatencyMin() < result.getLatencyMin()) {
                    result.setLatencyMin(bucket.getLatencyMin());
                }
            }
            if (bucket.getLatencyMax() != null) {
                if (result.getLatencyMax() == null || bucket.getLatencyMax() > result.getLatencyMax()) {
                    result.setLatencyMax(bucket.getLatencyMax());
                }
            }

            // Merge source metrics
            for (Map.Entry<String, MetricsBucket.SourceMetrics> entry : bucket.getBySource().entrySet()) {
                String sourceName = entry.getKey();
                MetricsBucket.SourceMetrics bucketSource = entry.getValue();

                MetricsBucket.SourceMetrics resultSource = result.getBySource()
                        .computeIfAbsent(sourceName, k -> new MetricsBucket.SourceMetrics());

                resultSource.setEvents(resultSource.getEvents() + bucketSource.getEvents());
                resultSource.setErrors(resultSource.getErrors() + bucketSource.getErrors());
                resultSource.setLatencySum(resultSource.getLatencySum() + bucketSource.getLatencySum());
                resultSource.setLatencyCount(resultSource.getLatencyCount() + bucketSource.getLatencyCount());
            }

            // Merge type metrics
            for (Map.Entry<String, MetricsBucket.TypeMetrics> entry : bucket.getByEventType().entrySet()) {
                String typeName = entry.getKey();
                MetricsBucket.TypeMetrics bucketType = entry.getValue();

                MetricsBucket.TypeMetrics resultType = result.getByEventType()
                        .computeIfAbsent(typeName, k -> new MetricsBucket.TypeMetrics());

                resultType.setCount(resultType.getCount() + bucketType.getCount());
                resultType.setLatencySum(resultType.getLatencySum() + bucketType.getLatencySum());
                resultType.setLatencyCount(resultType.getLatencyCount() + bucketType.getLatencyCount());
            }

            // Merge severity counts
            for (Map.Entry<String, Long> entry : bucket.getBySeverity().entrySet()) {
                String severity = entry.getKey();
                Long count = entry.getValue();

                result.getBySeverity().merge(severity, count, Long::sum);
            }

            // Collect latencies for percentile calculation
            String latencyKey = LATENCIES_PREFIX + t;
            Set<String> latencies = stringRedisTemplate.opsForZSet().range(latencyKey, 0, -1);
            if (latencies != null) {
                for (String l : latencies) {
                    try {
                        allLatencies.add(Long.parseLong(l));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        // Calculate percentiles from combined latencies
        if (!allLatencies.isEmpty()) {
            Collections.sort(allLatencies);
            int size = allLatencies.size();
            result.setLatencyP50((double) allLatencies.get((int) (size * 0.50)));
            result.setLatencyP95((double) allLatencies.get((int) (size * 0.95)));
            result.setLatencyP99((double) allLatencies.get(Math.min((int) (size * 0.99), size - 1)));
        }

        return result;
    }

    /**
     * Aggregate buckets for a SPECIFIC SOURCE only.
     * Returns a MetricsBucket where totalEvents/Errors come from that source's
     * data.
     */
    private MetricsBucket aggregateBucketsForSource(long startMs, long endMs, String source) {
        MetricsBucket result = new MetricsBucket(
                Instant.ofEpochMilli(startMs),
                Instant.ofEpochMilli(endMs));

        long bucketSizeMs = metricsProperties.getBucketSizeMs();
        long bucketStart = getBucketStart(startMs);

        for (long t = bucketStart; t <= endMs; t += bucketSizeMs) {
            String bucketKey = BUCKET_PREFIX + t;
            String sourceKey = bucketKey + ":source:" + source;

            // Get source-specific metrics
            long events = getLongFromHash(sourceKey, FIELD_EVENTS);
            long errors = getLongFromHash(sourceKey, FIELD_ERRORS);
            long latencySum = getLongFromHash(sourceKey, FIELD_LATENCY_SUM);
            long latencyCount = getLongFromHash(sourceKey, FIELD_LATENCY_COUNT);

            result.setTotalEvents(result.getTotalEvents() + events);
            result.setTotalErrors(result.getTotalErrors() + errors);
            result.setLatencySum(result.getLatencySum() + latencySum);
            result.setLatencyCount(result.getLatencyCount() + latencyCount);
        }

        return result;
    }

    /**
     * Aggregate buckets for a SPECIFIC EVENT TYPE only.
     * Returns a MetricsBucket where metrics come from that event type's data.
     */
    private MetricsBucket aggregateBucketsForEventType(long startMs, long endMs, String eventType) {
        MetricsBucket result = new MetricsBucket(
                Instant.ofEpochMilli(startMs),
                Instant.ofEpochMilli(endMs));

        long bucketSizeMs = metricsProperties.getBucketSizeMs();
        long bucketStart = getBucketStart(startMs);

        for (long t = bucketStart; t <= endMs; t += bucketSizeMs) {
            String bucketKey = BUCKET_PREFIX + t;
            String typeKey = bucketKey + ":type:" + eventType;

            // Get type-specific metrics
            long count = getLongFromHash(typeKey, FIELD_EVENTS);
            long latencySum = getLongFromHash(typeKey, FIELD_LATENCY_SUM);
            long latencyCount = getLongFromHash(typeKey, FIELD_LATENCY_COUNT);

            result.setTotalEvents(result.getTotalEvents() + count);
            result.setLatencySum(result.getLatencySum() + latencySum);
            result.setLatencyCount(result.getLatencyCount() + latencyCount);
        }

        return result;
    }

    /**
     * Calculate bucket start timestamp (aligned to bucket size).
     */
    private long getBucketStart(long timestamp) {
        long bucketSizeMs = metricsProperties.getBucketSizeMs();
        return (timestamp / bucketSizeMs) * bucketSizeMs;
    }

    /**
     * Update min/max latency atomically.
     */
    private void updateMinMax(String bucketKey, long latency) {
        // Get current min
        Long currentMin = getLongFromHashOrNull(bucketKey, FIELD_LATENCY_MIN);
        if (currentMin == null || latency < currentMin) {
            stringRedisTemplate.opsForHash().put(bucketKey, FIELD_LATENCY_MIN, String.valueOf(latency));
        }

        // Get current max
        Long currentMax = getLongFromHashOrNull(bucketKey, FIELD_LATENCY_MAX);
        if (currentMax == null || latency > currentMax) {
            stringRedisTemplate.opsForHash().put(bucketKey, FIELD_LATENCY_MAX, String.valueOf(latency));
        }
    }

    /**
     * Get percentile value from sorted set.
     */
    private Double getPercentile(String key, long size, double percentile) {
        long index = (long) (size * percentile);
        Set<String> result = stringRedisTemplate.opsForZSet().range(key, index, index);
        if (result != null && !result.isEmpty()) {
            try {
                return Double.parseDouble(result.iterator().next());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private long getLongFromHash(String key, String field) {
        Object value = stringRedisTemplate.opsForHash().get(key, field);
        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private Long getLongFromHashOrNull(String key, String field) {
        Object value = stringRedisTemplate.opsForHash().get(key, field);
        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Load per-source metrics from Redis keys.
     */
    private void loadSourceMetrics(String bucketKey, MetricsBucket bucket) {
        try {
            // Scan for all source keys: metrics:bucket:{timestamp}:source:*
            String pattern = bucketKey + ":source:*";
            Set<String> keys = stringRedisTemplate.keys(pattern);

            if (keys != null) {
                for (String key : keys) {
                    // Extract source name from key
                    String sourceName = key.substring(key.lastIndexOf(":source:") + 8);

                    MetricsBucket.SourceMetrics sourceMetrics = new MetricsBucket.SourceMetrics();
                    sourceMetrics.setEvents(getLongFromHash(key, FIELD_EVENTS));
                    sourceMetrics.setErrors(getLongFromHash(key, FIELD_ERRORS));
                    sourceMetrics.setLatencySum(getLongFromHash(key, FIELD_LATENCY_SUM));
                    sourceMetrics.setLatencyCount(getLongFromHash(key, FIELD_LATENCY_COUNT));

                    bucket.getBySource().put(sourceName, sourceMetrics);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load source metrics for bucket {}: {}", bucketKey, e.getMessage());
        }
    }

    /**
     * Load per-type metrics from Redis keys.
     */
    private void loadTypeMetrics(String bucketKey, MetricsBucket bucket) {
        try {
            // Scan for all type keys: metrics:bucket:{timestamp}:type:*
            String pattern = bucketKey + ":type:*";
            Set<String> keys = stringRedisTemplate.keys(pattern);

            if (keys != null) {
                for (String key : keys) {
                    // Extract type name from key
                    String typeName = key.substring(key.lastIndexOf(":type:") + 6);

                    MetricsBucket.TypeMetrics typeMetrics = new MetricsBucket.TypeMetrics();
                    typeMetrics.setCount(getLongFromHash(key, FIELD_EVENTS));
                    typeMetrics.setLatencySum(getLongFromHash(key, FIELD_LATENCY_SUM));
                    typeMetrics.setLatencyCount(getLongFromHash(key, FIELD_LATENCY_COUNT));

                    bucket.getByEventType().put(typeName, typeMetrics);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load type metrics for bucket {}: {}", bucketKey, e.getMessage());
        }
    }

    /**
     * Load severity breakdown from Redis.
     */
    private void loadSeverityMetrics(String bucketKey, MetricsBucket bucket) {
        try {
            String severityKey = bucketKey + ":severity";
            Map<Object, Object> severityData = stringRedisTemplate.opsForHash().entries(severityKey);

            if (severityData != null) {
                for (Map.Entry<Object, Object> entry : severityData.entrySet()) {
                    String severity = entry.getKey().toString();
                    try {
                        long count = Long.parseLong(entry.getValue().toString());
                        bucket.getBySeverity().put(severity, count);
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid severity count for {}: {}", severity, entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load severity metrics for bucket {}: {}", bucketKey, e.getMessage());
        }
    }
}
