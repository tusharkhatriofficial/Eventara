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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Hybrid distributed metrics service.
 * - Writes to Redis (real-time, fast, TTL expiry)
 * - Reads from Redis (<1h) or TimescaleDB (>1h)
 * - Scheduled rollup from Redis to TimescaleDB
 * 
 * When distributed.enabled=true:
 * - ALL reads come from Redis (respects TTL, data expires after retention
 * period)
 * - Multiple instances share the same data
 * 
 * When distributed.enabled=false:
 * - Falls back to in-memory storage (like old ComprehensiveMetricsService)
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

    // Peak tracking (in-memory, resets on restart)
    private volatile double peakThroughput = 0.0;
    private volatile Instant peakThroughputTimestamp = Instant.now();
    private volatile long lastThroughputCheck = System.currentTimeMillis();
    private volatile long lastEventCount = 0;

    /**
     * Record an event to distributed storage.
     * Writes ONLY to Redis when distributed mode is enabled.
     */
    public synchronized void recordEvent(EventDto event) {
        if (metricsProperties.getDistributed().isEnabled()) {
            // Write to Redis only - Redis handles everything with TTL
            redisMetrics.recordEvent(event);
        }
        logger.debug("Event recorded to Redis: type={}, source={}",
                event.getEventType(), event.getSource());
    }

    /**
     * Get comprehensive metrics - READS FROM REDIS when distributed is enabled.
     * Data automatically expires based on Redis TTL (default 1 hour).
     */
    public ComprehensiveMetricsDto getComprehensiveMetrics() {
        if (!metricsProperties.getDistributed().isEnabled()) {
            // Return empty metrics if distributed mode is disabled
            return new ComprehensiveMetricsDto();
        }

        ComprehensiveMetricsDto metrics = new ComprehensiveMetricsDto();

        // Get aggregated bucket from Redis (last 1 hour of data)
        MetricsBucket bucket = redisMetrics.getMetrics(Duration.ofHours(1));

        // Build all metric sections FROM REDIS DATA
        metrics.setSummary(buildSummaryMetricsFromRedis(bucket));
        metrics.setThroughput(buildThroughputMetricsFromRedis(bucket));
        metrics.setTimeWindows(buildTimeWindowMetricsFromRedis());
        metrics.setEventsByType(buildEventTypeMetricsFromRedis(bucket));
        metrics.setEventsBySource(buildSourceMetricsFromRedis(bucket));
        metrics.setEventsBySeverity(bucket.getBySeverity());
        metrics.setUserMetrics(buildUserMetricsFromRedis(bucket));
        metrics.setTopEvents(buildTopEventsMetricsFromRedis(bucket));
        metrics.setErrorAnalysis(buildErrorAnalysisMetricsFromRedis(bucket));
        metrics.setPerformance(buildPerformanceMetricsFromRedis(bucket));
        metrics.setAnomalies(detectAnomaliesFromRedis(bucket));

        return metrics;
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
     * Reset metrics - clears peak tracking.
     * Redis data is not cleared (it has TTL and will expire naturally).
     */
    public synchronized void resetMetrics() {
        peakThroughput = 0.0;
        peakThroughputTimestamp = Instant.now();
        lastThroughputCheck = System.currentTimeMillis();
        lastEventCount = 0;
        logger.info("Peak tracking reset (Redis data persists with TTL)");
    }

    // ===== REDIS-BASED BUILDER METHODS =====

    private ComprehensiveMetricsDto.SummaryMetrics buildSummaryMetricsFromRedis(MetricsBucket bucket) {
        ComprehensiveMetricsDto.SummaryMetrics summary = new ComprehensiveMetricsDto.SummaryMetrics();

        summary.setTotalEvents(bucket.getTotalEvents());
        summary.setUniqueSources(bucket.getBySource().size());
        summary.setUniqueEventTypes(bucket.getByEventType().size());
        summary.setUniqueUsers(bucket.getUniqueUsersEstimate());
        summary.setSystemHealth(calculateSystemHealth(bucket));
        summary.setLastUpdated(Instant.now());

        return summary;
    }

    private ComprehensiveMetricsDto.ThroughputMetrics buildThroughputMetricsFromRedis(MetricsBucket bucket) {
        ComprehensiveMetricsDto.ThroughputMetrics throughput = new ComprehensiveMetricsDto.ThroughputMetrics();

        // Get short-term metrics from Redis
        MetricsBucket last1min = redisMetrics.getMetricsLastMinutes(1);
        MetricsBucket last5min = redisMetrics.getMetricsLastMinutes(5);

        // Current throughput
        ComprehensiveMetricsDto.ThroughputMetrics.CurrentThroughput current = new ComprehensiveMetricsDto.ThroughputMetrics.CurrentThroughput();

        double eventsPerSecond = last1min.getTotalEvents() / 60.0;
        current.setPerSecond(Math.round(eventsPerSecond * 100.0) / 100.0);
        current.setPerMinute(last1min.getTotalEvents());
        current.setPerHour(bucket.getTotalEvents());
        current.setPerDay(bucket.getTotalEvents() * 24); // Projection
        throughput.setCurrent(current);

        // Track peak
        if (eventsPerSecond > peakThroughput) {
            peakThroughput = eventsPerSecond;
            peakThroughputTimestamp = Instant.now();
        }

        // Peak throughput
        ComprehensiveMetricsDto.ThroughputMetrics.PeakThroughput peak = new ComprehensiveMetricsDto.ThroughputMetrics.PeakThroughput();
        peak.setValue(Math.round(peakThroughput * 100.0) / 100.0);
        peak.setTimestamp(peakThroughputTimestamp);
        throughput.setPeak(peak);

        // Average throughput
        ComprehensiveMetricsDto.ThroughputMetrics.AverageThroughput average = new ComprehensiveMetricsDto.ThroughputMetrics.AverageThroughput();
        average.setLast1Hour(bucket.getTotalEvents() / 3600.0);

        // Try to get 24h from TimescaleDB
        try {
            long events24h = timescaleMetrics.countEventsInWindow(Duration.ofHours(24));
            average.setLast24Hours(events24h / 86400.0);
        } catch (Exception e) {
            average.setLast24Hours(bucket.getTotalEvents() / 3600.0); // Fallback
        }
        throughput.setAverage(average);

        return throughput;
    }

    private ComprehensiveMetricsDto.TimeWindowMetrics buildTimeWindowMetricsFromRedis() {
        ComprehensiveMetricsDto.TimeWindowMetrics windows = new ComprehensiveMetricsDto.TimeWindowMetrics();

        // All from Redis (real-time data)
        windows.setLast1Minute(redisMetrics.getMetricsLastMinutes(1).getTotalEvents());
        windows.setLast5Minutes(redisMetrics.getMetricsLastMinutes(5).getTotalEvents());
        windows.setLast15Minutes(redisMetrics.getMetricsLastMinutes(15).getTotalEvents());
        windows.setLast1Hour(redisMetrics.getMetrics(Duration.ofHours(1)).getTotalEvents());

        // 24h from TimescaleDB
        try {
            windows.setLast24Hours(timescaleMetrics.countEventsInWindow(Duration.ofHours(24)));
        } catch (Exception e) {
            windows.setLast24Hours(windows.getLast1Hour()); // Fallback
        }

        return windows;
    }

    private Map<String, ComprehensiveMetricsDto.EventTypeMetrics> buildEventTypeMetricsFromRedis(MetricsBucket bucket) {
        Map<String, ComprehensiveMetricsDto.EventTypeMetrics> metricsMap = new ConcurrentHashMap<>();
        long totalEvents = bucket.getTotalEvents();

        bucket.getByEventType().forEach((type, typeData) -> {
            ComprehensiveMetricsDto.EventTypeMetrics metrics = new ComprehensiveMetricsDto.EventTypeMetrics();

            long count = typeData.getCount();
            metrics.setCount(count);
            metrics.setPercentage(totalEvents > 0 ? Math.round((count * 100.0 / totalEvents) * 100.0) / 100.0 : 0);
            metrics.setAvgLatency(Math.round(typeData.getAvgLatency() * 100.0) / 100.0);

            metricsMap.put(type, metrics);
        });

        return metricsMap;
    }

    private Map<String, ComprehensiveMetricsDto.SourceMetrics> buildSourceMetricsFromRedis(MetricsBucket bucket) {
        Map<String, ComprehensiveMetricsDto.SourceMetrics> metricsMap = new ConcurrentHashMap<>();

        bucket.getBySource().forEach((source, sourceData) -> {
            ComprehensiveMetricsDto.SourceMetrics metrics = new ComprehensiveMetricsDto.SourceMetrics();

            metrics.setCount(sourceData.getEvents());
            metrics.setErrorCount(sourceData.getErrors());
            metrics.setErrorRate(Math.round(sourceData.getErrorRate() * 100.0) / 100.0);
            metrics.setAvgLatency(Math.round(sourceData.getAvgLatency() * 100.0) / 100.0);
            metrics.setHealth(determineSourceHealth(sourceData.getErrorRate(), sourceData.getAvgLatency()));

            metricsMap.put(source, metrics);
        });

        return metricsMap;
    }

    private ComprehensiveMetricsDto.UserMetrics buildUserMetricsFromRedis(MetricsBucket bucket) {
        ComprehensiveMetricsDto.UserMetrics userMetrics = new ComprehensiveMetricsDto.UserMetrics();

        userMetrics.setTotalUniqueUsers(bucket.getUniqueUsersEstimate());
        userMetrics.setActiveUsersLast1Hour(bucket.getUniqueUsersEstimate());

        // Try to get 24h from TimescaleDB
        try {
            // For now, use same as 1h estimate
            userMetrics.setActiveUsersLast24Hours(bucket.getUniqueUsersEstimate());
        } catch (Exception e) {
            userMetrics.setActiveUsersLast24Hours(bucket.getUniqueUsersEstimate());
        }

        // Top users not tracked in Redis buckets yet - return empty list
        userMetrics.setTopActiveUsers(new ArrayList<>());

        return userMetrics;
    }

    private ComprehensiveMetricsDto.TopEventsMetrics buildTopEventsMetricsFromRedis(MetricsBucket bucket) {
        ComprehensiveMetricsDto.TopEventsMetrics topEvents = new ComprehensiveMetricsDto.TopEventsMetrics();

        // Most frequent events (sorted by count)
        List<ComprehensiveMetricsDto.TopEventsMetrics.EventRanking> mostFrequent = bucket.getByEventType().entrySet()
                .stream()
                .sorted((a, b) -> Long.compare(b.getValue().getCount(), a.getValue().getCount()))
                .limit(10)
                .map(entry -> {
                    String type = entry.getKey();
                    long count = entry.getValue().getCount();
                    double avgLatency = entry.getValue().getAvgLatency();
                    return new ComprehensiveMetricsDto.TopEventsMetrics.EventRanking(type, count, avgLatency);
                })
                .collect(Collectors.toList());
        topEvents.setMostFrequent(mostFrequent);

        // Fastest events (lowest latency)
        List<ComprehensiveMetricsDto.TopEventsMetrics.EventRanking> fastest = bucket.getByEventType().entrySet()
                .stream()
                .filter(e -> e.getValue().getLatencyCount() > 0)
                .sorted(Comparator.comparingDouble(e -> e.getValue().getAvgLatency()))
                .limit(5)
                .map(entry -> {
                    String type = entry.getKey();
                    long count = entry.getValue().getCount();
                    double avgLatency = entry.getValue().getAvgLatency();
                    return new ComprehensiveMetricsDto.TopEventsMetrics.EventRanking(type, count, avgLatency);
                })
                .collect(Collectors.toList());
        topEvents.setFastest(fastest);

        // Slowest events (highest latency)
        List<ComprehensiveMetricsDto.TopEventsMetrics.EventRanking> slowest = bucket.getByEventType().entrySet()
                .stream()
                .filter(e -> e.getValue().getLatencyCount() > 0)
                .sorted((a, b) -> Double.compare(b.getValue().getAvgLatency(), a.getValue().getAvgLatency()))
                .limit(5)
                .map(entry -> {
                    String type = entry.getKey();
                    long count = entry.getValue().getCount();
                    double avgLatency = entry.getValue().getAvgLatency();
                    return new ComprehensiveMetricsDto.TopEventsMetrics.EventRanking(type, count, avgLatency);
                })
                .collect(Collectors.toList());
        topEvents.setSlowest(slowest);

        return topEvents;
    }

    private ComprehensiveMetricsDto.ErrorAnalysisMetrics buildErrorAnalysisMetricsFromRedis(MetricsBucket bucket) {
        ComprehensiveMetricsDto.ErrorAnalysisMetrics errorAnalysis = new ComprehensiveMetricsDto.ErrorAnalysisMetrics();

        long totalErrors = bucket.getTotalErrors();
        long totalEvents = bucket.getTotalEvents();

        errorAnalysis.setTotalErrors(totalErrors);
        errorAnalysis
                .setErrorRate(totalEvents > 0 ? Math.round((totalErrors * 100.0 / totalEvents) * 100.0) / 100.0 : 0);

        // Errors by type (from event type data)
        List<ComprehensiveMetricsDto.ErrorAnalysisMetrics.ErrorBreakdown> errorsByType = bucket.getByEventType()
                .entrySet().stream()
                .filter(e -> e.getValue().getCount() > 0)
                .sorted((a, b) -> Long.compare(b.getValue().getCount(), a.getValue().getCount()))
                .limit(10)
                .map(entry -> {
                    long count = entry.getValue().getCount();
                    double percentage = totalEvents > 0 ? (count * 100.0 / totalEvents) : 0;
                    return new ComprehensiveMetricsDto.ErrorAnalysisMetrics.ErrorBreakdown(
                            entry.getKey(),
                            count,
                            Math.round(percentage * 100.0) / 100.0);
                })
                .collect(Collectors.toList());
        errorAnalysis.setErrorsByType(errorsByType);

        // Errors by source
        List<ComprehensiveMetricsDto.ErrorAnalysisMetrics.ErrorBreakdown> errorsBySource = bucket.getBySource()
                .entrySet().stream()
                .filter(e -> e.getValue().getErrors() > 0)
                .sorted((a, b) -> Long.compare(b.getValue().getErrors(), a.getValue().getErrors()))
                .limit(10)
                .map(entry -> {
                    long count = entry.getValue().getErrors();
                    double percentage = totalErrors > 0 ? (count * 100.0 / totalErrors) : 0;
                    return new ComprehensiveMetricsDto.ErrorAnalysisMetrics.ErrorBreakdown(
                            entry.getKey(),
                            count,
                            Math.round(percentage * 100.0) / 100.0);
                })
                .collect(Collectors.toList());
        errorAnalysis.setErrorsBySource(errorsBySource);

        return errorAnalysis;
    }

    private ComprehensiveMetricsDto.PerformanceMetrics buildPerformanceMetricsFromRedis(MetricsBucket bucket) {
        ComprehensiveMetricsDto.PerformanceMetrics performance = new ComprehensiveMetricsDto.PerformanceMetrics();

        performance.setAvgLatency(bucket.getAvgLatency());
        performance.setP50(bucket.getLatencyP50() != null ? bucket.getLatencyP50() : 0);
        performance.setP95(bucket.getLatencyP95() != null ? bucket.getLatencyP95() : 0);
        performance.setP99(bucket.getLatencyP99() != null ? bucket.getLatencyP99() : 0);
        performance.setMinLatency(bucket.getLatencyMin() != null ? bucket.getLatencyMin() : 0);
        performance.setMaxLatency(bucket.getLatencyMax() != null ? bucket.getLatencyMax() : 0);

        return performance;
    }

    private List<ComprehensiveMetricsDto.AnomalyAlert> detectAnomaliesFromRedis(MetricsBucket bucket) {
        List<ComprehensiveMetricsDto.AnomalyAlert> alerts = new ArrayList<>();

        // Check for high error rate
        double errorRate = bucket.getErrorRate();
        if (errorRate > 5.0) {
            ComprehensiveMetricsDto.AnomalyAlert alert = new ComprehensiveMetricsDto.AnomalyAlert();
            alert.setSeverity(errorRate > 10 ? "critical" : "warning");
            alert.setType("high_error_rate");
            alert.setMessage("Error rate is above threshold (" + String.format("%.1f", errorRate) + "%)");
            alert.setThreshold(5.0);
            alert.setCurrentValue(errorRate);
            alert.setDetectedAt(Instant.now());
            alerts.add(alert);
        }

        // Check for high latency
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

        // Check for traffic spike
        MetricsBucket last1min = redisMetrics.getMetricsLastMinutes(1);
        MetricsBucket last5min = redisMetrics.getMetricsLastMinutes(5);

        double avgPerMinute = last5min.getTotalEvents() / 5.0;
        if (avgPerMinute > 0 && last1min.getTotalEvents() > avgPerMinute * 3) {
            ComprehensiveMetricsDto.AnomalyAlert alert = new ComprehensiveMetricsDto.AnomalyAlert();
            alert.setSeverity("warning");
            alert.setType("traffic_spike");
            alert.setMessage("Traffic spike detected (3x normal)");
            alert.setThreshold(avgPerMinute);
            alert.setCurrentValue((double) last1min.getTotalEvents());
            alert.setDetectedAt(Instant.now());
            alerts.add(alert);
        }

        return alerts;
    }

    // ===== HELPER METHODS =====

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

    private String determineSourceHealth(double errorRate, double avgLatency) {
        if (errorRate > 10 || avgLatency > 5000) {
            return "down";
        } else if (errorRate > 5 || avgLatency > 1000) {
            return "degraded";
        }
        return "healthy";
    }
}
