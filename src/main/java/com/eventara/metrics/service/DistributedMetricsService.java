package com.eventara.metrics.service;

import com.eventara.common.dto.ComprehensiveMetricsDto;
import com.eventara.common.dto.EventDto;
import com.eventara.metrics.config.MetricsProperties;
import com.eventara.metrics.model.MetricsBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Hybrid distributed metrics service.
 * - Writes to Redis (real-time, fast, TTL expiry)
 * - Reads from Redis (<1h) or TimescaleDB (>1h)
 * - Scheduled rollup from Redis to TimescaleDB
 * 
 * This service provides the same interface as ComprehensiveMetricsService
 * for backward compatibility with existing controllers.
 */
@Service
public class DistributedMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(DistributedMetricsService.class);

    @Autowired
    private RedisMetricsService redisMetrics;

    @Autowired
    private TimescaleMetricsService timescaleMetrics;

    @Autowired
    private MetricsProperties metricsProperties;

    // In-memory tracking for unique counts (approximate)
    private final Map<String, AtomicLong> uniqueUsers = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> uniqueSources = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> uniqueEventTypes = new ConcurrentHashMap<>();

    // Peak tracking
    private volatile double peakThroughput = 0.0;
    private volatile Instant peakThroughputTimestamp = Instant.now();

    /**
     * Record an event to distributed storage.
     * Delegates to Redis for real-time storage.
     */
    public synchronized void recordEvent(EventDto event) {
        redisMetrics.recordEvent(event);

        // Track unique counts locally (approximate)
        if (event.getUserId() != null) {
            uniqueUsers.computeIfAbsent(event.getUserId(), k -> new AtomicLong(0)).incrementAndGet();
        }
        if (event.getSource() != null) {
            uniqueSources.computeIfAbsent(event.getSource(), k -> new AtomicLong(0)).incrementAndGet();
        }
        if (event.getEventType() != null) {
            uniqueEventTypes.computeIfAbsent(event.getEventType(), k -> new AtomicLong(0)).incrementAndGet();
        }
    }

    /**
     * Get comprehensive metrics (same interface as ComprehensiveMetricsService).
     */
    public ComprehensiveMetricsDto getComprehensiveMetrics() {
        ComprehensiveMetricsDto dto = new ComprehensiveMetricsDto();

        // Get current metrics from Redis (last 1 hour)
        MetricsBucket currentBucket = redisMetrics.getMetrics(Duration.ofHours(1));

        // Build all metric sections
        dto.setSummary(buildSummaryMetrics(currentBucket));
        dto.setThroughput(buildThroughputMetrics(currentBucket));
        dto.setTimeWindows(buildTimeWindowMetrics());
        dto.setEventsByType(buildEventTypeMetrics(currentBucket));
        dto.setEventsBySource(buildSourceMetrics(currentBucket));
        dto.setEventsBySeverity(currentBucket.getBySeverity());
        dto.setUserMetrics(buildUserMetrics());
        dto.setErrorAnalysis(buildErrorAnalysisMetrics(currentBucket));
        dto.setPerformance(buildPerformanceMetrics(currentBucket));
        dto.setAnomalies(detectAnomalies(currentBucket));

        return dto;
    }

    /**
     * Scheduled rollup: Redis → TimescaleDB.
     * Runs every minute by default.
     */
    @Scheduled(fixedRateString = "${eventara.metrics.rollup.interval-seconds:60}000")
    public void rollupToTimescale() {
        if (!metricsProperties.getDistributed().isEnabled()) {
            return;
        }

        try {
            Instant end = Instant.now().minusSeconds(60); // 1 minute ago
            Instant start = end.minusSeconds(metricsProperties.getRollup().getIntervalSeconds());

            List<MetricsBucket> buckets = redisMetrics.getBuckets(start, end);
            if (!buckets.isEmpty()) {
                timescaleMetrics.insertBuckets(buckets);
                logger.info("Rolled up {} buckets from Redis to TimescaleDB", buckets.size());
            }
        } catch (Exception e) {
            logger.error("Failed to rollup metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Reset metrics (for backward compatibility).
     */
    public synchronized void resetMetrics() {
        uniqueUsers.clear();
        uniqueSources.clear();
        uniqueEventTypes.clear();
        peakThroughput = 0.0;
        peakThroughputTimestamp = Instant.now();
        logger.info("Distributed metrics tracking reset (Redis data persists with TTL)");
    }

    // ===== Private builder methods =====

    private ComprehensiveMetricsDto.SummaryMetrics buildSummaryMetrics(MetricsBucket bucket) {
        ComprehensiveMetricsDto.SummaryMetrics summary = new ComprehensiveMetricsDto.SummaryMetrics();
        summary.setTotalEvents(bucket.getTotalEvents());
        summary.setUniqueSources(uniqueSources.size());
        summary.setUniqueEventTypes(uniqueEventTypes.size());
        summary.setUniqueUsers(uniqueUsers.size());
        summary.setSystemHealth(calculateSystemHealth(bucket));
        summary.setLastUpdated(Instant.now());
        return summary;
    }

    private ComprehensiveMetricsDto.ThroughputMetrics buildThroughputMetrics(MetricsBucket bucket) {
        ComprehensiveMetricsDto.ThroughputMetrics throughput = new ComprehensiveMetricsDto.ThroughputMetrics();

        // Current throughput
        ComprehensiveMetricsDto.ThroughputMetrics.CurrentThroughput current = new ComprehensiveMetricsDto.ThroughputMetrics.CurrentThroughput();

        // Get short-term metrics
        MetricsBucket last1min = redisMetrics.getMetricsLastMinutes(1);
        MetricsBucket last5min = redisMetrics.getMetricsLastMinutes(5);

        double eventsPerSecond = last1min.getTotalEvents() / 60.0;
        current.setPerSecond(Math.round(eventsPerSecond * 100.0) / 100.0);
        current.setPerMinute(last1min.getTotalEvents());
        current.setPerHour(bucket.getTotalEvents());
        current.setPerDay(bucket.getTotalEvents() * 24); // Projection
        throughput.setCurrent(current);

        // Update peak if needed
        if (eventsPerSecond > peakThroughput) {
            peakThroughput = eventsPerSecond;
            peakThroughputTimestamp = Instant.now();
        }

        // Peak throughput
        ComprehensiveMetricsDto.ThroughputMetrics.PeakThroughput peak = new ComprehensiveMetricsDto.ThroughputMetrics.PeakThroughput();
        peak.setValue(peakThroughput);
        peak.setTimestamp(peakThroughputTimestamp);
        throughput.setPeak(peak);

        // Average throughput
        ComprehensiveMetricsDto.ThroughputMetrics.AverageThroughput average = new ComprehensiveMetricsDto.ThroughputMetrics.AverageThroughput();
        average.setLast1Hour(bucket.getTotalEvents() / 60.0);
        average.setLast24Hours(bucket.getTotalEvents() / 60.0); // Same for now (Redis only has 1h)
        throughput.setAverage(average);

        return throughput;
    }

    private ComprehensiveMetricsDto.TimeWindowMetrics buildTimeWindowMetrics() {
        ComprehensiveMetricsDto.TimeWindowMetrics windows = new ComprehensiveMetricsDto.TimeWindowMetrics();

        // Get from Redis (<1h)
        windows.setLast1Minute(redisMetrics.getMetricsLastMinutes(1).getTotalEvents());
        windows.setLast5Minutes(redisMetrics.getMetricsLastMinutes(5).getTotalEvents());
        windows.setLast15Minutes(redisMetrics.getMetricsLastMinutes(15).getTotalEvents());
        windows.setLast1Hour(redisMetrics.getMetrics(Duration.ofHours(1)).getTotalEvents());

        // Get from TimescaleDB (>1h)
        try {
            windows.setLast24Hours(timescaleMetrics.countEventsInWindow(Duration.ofHours(24)));
        } catch (Exception e) {
            windows.setLast24Hours(windows.getLast1Hour()); // Fallback
        }

        return windows;
    }

    private Map<String, ComprehensiveMetricsDto.EventTypeMetrics> buildEventTypeMetrics(MetricsBucket bucket) {
        return uniqueEventTypes.keySet().stream()
                .collect(Collectors.toMap(
                        type -> type,
                        type -> {
                            ComprehensiveMetricsDto.EventTypeMetrics metrics = new ComprehensiveMetricsDto.EventTypeMetrics();
                            MetricsBucket.TypeMetrics typeData = bucket.getByEventType().get(type);
                            if (typeData != null) {
                                metrics.setCount(typeData.getCount());
                                metrics.setAvgLatency(typeData.getAvgLatency());
                                metrics.setPercentage(bucket.getTotalEvents() > 0
                                        ? (typeData.getCount() * 100.0 / bucket.getTotalEvents())
                                        : 0);
                            } else {
                                metrics.setCount(uniqueEventTypes.get(type).get());
                                metrics.setPercentage(0);
                                metrics.setAvgLatency(0);
                            }
                            return metrics;
                        }));
    }

    private Map<String, ComprehensiveMetricsDto.SourceMetrics> buildSourceMetrics(MetricsBucket bucket) {
        return uniqueSources.keySet().stream()
                .collect(Collectors.toMap(
                        source -> source,
                        source -> {
                            ComprehensiveMetricsDto.SourceMetrics metrics = new ComprehensiveMetricsDto.SourceMetrics();
                            MetricsBucket.SourceMetrics sourceData = bucket.getBySource().get(source);
                            if (sourceData != null) {
                                metrics.setCount(sourceData.getEvents());
                                metrics.setErrorCount(sourceData.getErrors());
                                metrics.setErrorRate(sourceData.getErrorRate());
                                metrics.setAvgLatency(sourceData.getAvgLatency());
                                metrics.setHealth(
                                        calculateSourceHealth(sourceData.getErrorRate(), sourceData.getAvgLatency()));
                            } else {
                                metrics.setCount(uniqueSources.get(source).get());
                                metrics.setHealth("unknown");
                            }
                            return metrics;
                        }));
    }

    private ComprehensiveMetricsDto.UserMetrics buildUserMetrics() {
        ComprehensiveMetricsDto.UserMetrics userMetrics = new ComprehensiveMetricsDto.UserMetrics();
        userMetrics.setTotalUniqueUsers(uniqueUsers.size());
        userMetrics.setActiveUsersLast1Hour(uniqueUsers.size()); // All users are "active" in last hour
        userMetrics.setActiveUsersLast24Hours(uniqueUsers.size());

        // Top active users
        List<ComprehensiveMetricsDto.UserMetrics.UserActivity> topUsers = uniqueUsers.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                .limit(10)
                .map(e -> new ComprehensiveMetricsDto.UserMetrics.UserActivity(e.getKey(), e.getValue().get()))
                .collect(Collectors.toList());
        userMetrics.setTopActiveUsers(topUsers);

        return userMetrics;
    }

    private ComprehensiveMetricsDto.ErrorAnalysisMetrics buildErrorAnalysisMetrics(MetricsBucket bucket) {
        ComprehensiveMetricsDto.ErrorAnalysisMetrics errorMetrics = new ComprehensiveMetricsDto.ErrorAnalysisMetrics();
        errorMetrics.setTotalErrors(bucket.getTotalErrors());
        errorMetrics.setErrorRate(bucket.getErrorRate());
        return errorMetrics;
    }

    private ComprehensiveMetricsDto.PerformanceMetrics buildPerformanceMetrics(MetricsBucket bucket) {
        ComprehensiveMetricsDto.PerformanceMetrics perf = new ComprehensiveMetricsDto.PerformanceMetrics();
        perf.setAvgLatency(bucket.getAvgLatency());
        perf.setP50(bucket.getLatencyP50() != null ? bucket.getLatencyP50() : 0);
        perf.setP95(bucket.getLatencyP95() != null ? bucket.getLatencyP95() : 0);
        perf.setP99(bucket.getLatencyP99() != null ? bucket.getLatencyP99() : 0);
        perf.setMinLatency(bucket.getLatencyMin() != null ? bucket.getLatencyMin() : 0);
        perf.setMaxLatency(bucket.getLatencyMax() != null ? bucket.getLatencyMax() : 0);
        return perf;
    }

    private List<ComprehensiveMetricsDto.AnomalyAlert> detectAnomalies(MetricsBucket bucket) {
        java.util.ArrayList<ComprehensiveMetricsDto.AnomalyAlert> alerts = new java.util.ArrayList<>();

        // Check error rate
        double errorRate = bucket.getErrorRate();
        if (errorRate > 5.0) {
            ComprehensiveMetricsDto.AnomalyAlert alert = new ComprehensiveMetricsDto.AnomalyAlert();
            alert.setSeverity(errorRate > 10 ? "critical" : "warning");
            alert.setType("high_error_rate");
            alert.setMessage("Error rate is above threshold (" + String.format("%.1f", errorRate) + "%)");
            alert.setThreshold(5.0);
            alert.setCurrentValue(Math.round(errorRate * 100.0) / 100.0);
            alert.setDetectedAt(Instant.now());
            alerts.add(alert);
        }

        // Check latency
        Double p95 = bucket.getLatencyP95();
        if (p95 != null && p95 > 1000) {
            ComprehensiveMetricsDto.AnomalyAlert alert = new ComprehensiveMetricsDto.AnomalyAlert();
            alert.setSeverity(p95 > 5000 ? "critical" : "warning");
            alert.setType("high_latency");
            alert.setMessage("P95 latency is above threshold (" + p95.intValue() + "ms)");
            alert.setThreshold(1000.0);
            alert.setCurrentValue(p95);
            alert.setDetectedAt(Instant.now());
            alerts.add(alert);
        }

        return alerts;
    }

    private String calculateSystemHealth(MetricsBucket bucket) {
        double errorRate = bucket.getErrorRate();
        Double p95 = bucket.getLatencyP95();

        if (errorRate > 10 || (p95 != null && p95 > 5000)) {
            return "critical";
        } else if (errorRate > 5 || (p95 != null && p95 > 1000)) {
            return "degraded";
        }
        return "healthy";
    }

    private String calculateSourceHealth(double errorRate, double avgLatency) {
        if (errorRate > 10 || avgLatency > 5000) {
            return "critical";
        } else if (errorRate > 5 || avgLatency > 1000) {
            return "degraded";
        }
        return "healthy";
    }
}
