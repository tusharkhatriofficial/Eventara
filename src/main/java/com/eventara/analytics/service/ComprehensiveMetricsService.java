package com.eventara.analytics.service;

import com.eventara.common.dto.ComprehensiveMetricsDto;
import com.eventara.common.dto.EventDto;
import com.eventara.common.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
public class ComprehensiveMetricsService {

    public static final Logger logger = LoggerFactory.getLogger(ComprehensiveMetricsService.class);

    private EventRepository eventRepository;

    //core counters
    private final AtomicLong totalEvents = new AtomicLong(0);

    //Event tracking maps
    private final Map<String, AtomicLong> eventsByType = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> eventsBySource = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> eventsBySeverity = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> eventsByUser = new ConcurrentHashMap<>();

    //latency tracking for performance metrics
    private final Map<String, List<Long>> latenciesByType = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> latenciesBySource = new ConcurrentHashMap<>();
    private final List<Long> allLatencies = Collections.synchronizedList(new ArrayList<>());

    //Error tracking
    private final Map<String, AtomicLong> errorsByType = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorsBySource = new ConcurrentHashMap<>();

    //timestamp tracking for time windows
    private final Queue<TimestampedEvent> eventTimestamps = new LinkedList<>();

    // Throughput tracking
    private long lastMetricsTimestamp = System.currentTimeMillis();
    private long lastEventCount = 0;
    private double peakThroughput = 0.0;
    private Instant peakThroughputTimestamp = Instant.now();

    // Helper class to track event details over time
    private static class TimestampedEvent {
        long timestamp;
        String eventType;
        String source;
        String userId;
        String severity;
        long latency;

        TimestampedEvent(long timestamp, String eventType, String source, String userId, String severity, long latency) {
            this.timestamp = timestamp;
            this.eventType = eventType;
            this.source = source;
            this.userId = userId;
            this.severity = severity;
            this.latency = latency;
        }
    }

    public synchronized void recordEvent(EventDto event){
        long now = System.currentTimeMillis();

        //basic counters
        totalEvents.incrementAndGet();

        //count by type
        eventsByType.computeIfAbsent(event.getEventType(), k -> new AtomicLong(0)).incrementAndGet();

        //count by source
        eventsBySource.computeIfAbsent(event.getSource(), k -> new AtomicLong(0)).incrementAndGet();

        //count by severity
        if(event.getSeverity() != null){
            eventsBySeverity.computeIfAbsent(event.getSeverity(), k -> new AtomicLong(0)).incrementAndGet();
        }

        //count by user
        if(event.getUserId() != null){
            eventsByUser.computeIfAbsent(event.getUserId(), k -> new AtomicLong(0)).incrementAndGet();
        }

        //calculate and track latency
        long latency = event.getProcessingLatencyMs();
        if (latency>0){
            allLatencies.add(latency);

            //tracking laterncy by type
            latenciesByType.computeIfAbsent(event.getEventType(), k -> Collections.synchronizedList(new ArrayList<>())).add(latency);

            // Track latency by source
            latenciesBySource.computeIfAbsent(event.getSource(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(latency);

            //limiting list size to prevent memmory issues (Stores only last 1000 latencies)
            if(allLatencies.size() > 1000){
                allLatencies.remove(0);
            }
        }
        //previous if statement ends above this line

        //track errors
        if(event.isError()){
            errorsByType.computeIfAbsent(event.getEventType(), k -> new AtomicLong(0)).incrementAndGet();
            errorsBySource.computeIfAbsent(event.getSource(), k -> new AtomicLong(0))
                    .incrementAndGet();
        }
        //previous if statement ends above this line

        // Track timestamp for time windows
        TimestampedEvent timestampedEvent = new TimestampedEvent(
          now,
          event.getEventType(),
          event.getSource(),
          event.getUserId(),
          event.getSeverity(),
                latency
        );
        eventTimestamps.offer(timestampedEvent);

        // Clean old events (older than 24 hours)
        long cutoff = now - (24 * 60 * 60 * 1000);
        while (!eventTimestamps.isEmpty() && eventTimestamps.peek().timestamp < cutoff){
            eventTimestamps.poll();
        }

        logger.debug("Event recorded: type={}, source={}, user={}, latency={}ms",
                event.getEventType(), event.getSource(), event.getUserId(), latency);

    }

    //Get comprehensive metrics snapshot

    public ComprehensiveMetricsDto getComprehensiveMetrics(){
        ComprehensiveMetricsDto metrics = new ComprehensiveMetricsDto();

        // 1. Summary Metrics
        metrics.setSummary(buildSummaryMetrics());

        // 2. Throughput Metrics
        metrics.setThroughput(buildThroughputMetrics());

        // 3. Time Window Metrics
        metrics.setTimeWindows(buildTimeWindowMetrics());

        // 4. Events by Type
        metrics.setEventsByType(buildEventTypeMetrics());

        // 5. Events by Source
        metrics.setEventsBySource(buildSourceMetrics());

        // 6. Events by Severity
        Map<String, Long> severityMap = new ConcurrentHashMap<>();
        eventsBySeverity.forEach((k, v) -> severityMap.put(k, v.get()));
        metrics.setEventsBySeverity(severityMap);

        // 7. User Metrics
        metrics.setUserMetrics(buildUserMetrics());

        // 8. Top Events
        metrics.setTopEvents(buildTopEventsMetrics());

        // 9. Error Analysis
        metrics.setErrorAnalysis(buildErrorAnalysisMetrics());

        // 10. Performance Metrics
        metrics.setPerformance(buildPerformanceMetrics());

        // 11. Anomaly Detection
        metrics.setAnomalies(detectAnomalies());

        return metrics;
    }

    //builder methods
    private ComprehensiveMetricsDto.SummaryMetrics buildSummaryMetrics(){
        ComprehensiveMetricsDto.SummaryMetrics summary = new ComprehensiveMetricsDto.SummaryMetrics();

        summary.setTotalEvents(totalEvents.get());
        summary.setUniqueSources(eventsBySource.size());
        summary.setUniqueEventTypes(eventsByType.size());
        summary.setUniqueUsers(eventsByUser.size());
        summary.setSystemHealth(calculateSystemHealth());
        summary.setLastUpdated(Instant.now());

        return summary;
    }

    private ComprehensiveMetricsDto.ThroughputMetrics buildThroughputMetrics() {
        ComprehensiveMetricsDto.ThroughputMetrics throughput = new ComprehensiveMetricsDto.ThroughputMetrics();

        long now = System.currentTimeMillis();
        long timeDiffMs = now - lastMetricsTimestamp;
        long eventDiff = totalEvents.get() - lastEventCount;

        // Current throughput
        ComprehensiveMetricsDto.ThroughputMetrics.CurrentThroughput current =
                new ComprehensiveMetricsDto.ThroughputMetrics.CurrentThroughput();

        if (timeDiffMs > 0) {
            double perSecond = (eventDiff * 1000.0) / timeDiffMs;
            current.setPerSecond(Math.round(perSecond * 100.0) / 100.0);
            current.setPerMinute(Math.round(perSecond * 60 * 100.0) / 100.0);
            current.setPerHour(Math.round(perSecond * 3600 * 100.0) / 100.0);
            current.setPerDay(Math.round(perSecond * 86400 * 100.0) / 100.0);

            // Track peak
            if (perSecond > peakThroughput) {
                peakThroughput = perSecond;
                peakThroughputTimestamp = Instant.now();
            }
        }
        throughput.setCurrent(current);

        // Peak throughput
        ComprehensiveMetricsDto.ThroughputMetrics.PeakThroughput peak =
                new ComprehensiveMetricsDto.ThroughputMetrics.PeakThroughput();
        peak.setValue(Math.round(peakThroughput * 100.0) / 100.0);
        peak.setTimestamp(peakThroughputTimestamp);
        throughput.setPeak(peak);

        // Average throughput
        ComprehensiveMetricsDto.ThroughputMetrics.AverageThroughput average =
                new ComprehensiveMetricsDto.ThroughputMetrics.AverageThroughput();

        long eventsLast1Hour = countEventsInWindow(now, 60 * 60 * 1000);
        long eventsLast24Hours = countEventsInWindow(now, 24 * 60 * 60 * 1000);

        average.setLast1Hour(Math.round((eventsLast1Hour / 3600.0) * 100.0) / 100.0);
        average.setLast24Hours(Math.round((eventsLast24Hours / 86400.0) * 100.0) / 100.0);
        throughput.setAverage(average);

        // Update for next calculation
        lastMetricsTimestamp = now;
        lastEventCount = totalEvents.get();

        return throughput;
    }


    private ComprehensiveMetricsDto.TimeWindowMetrics buildTimeWindowMetrics() {
        long now = System.currentTimeMillis();
        ComprehensiveMetricsDto.TimeWindowMetrics windows = new ComprehensiveMetricsDto.TimeWindowMetrics();

        windows.setLast1Minute(countEventsInWindow(now, 1 * 60 * 1000));
        windows.setLast5Minutes(countEventsInWindow(now, 5 * 60 * 1000));
        windows.setLast15Minutes(countEventsInWindow(now, 15 * 60 * 1000));
        windows.setLast1Hour(countEventsInWindow(now, 60 * 60 * 1000));
        windows.setLast24Hours(countEventsInWindow(now, 24 * 60 * 60 * 1000));

        return windows;
    }

    private Map<String, ComprehensiveMetricsDto.EventTypeMetrics> buildEventTypeMetrics() {
        Map<String, ComprehensiveMetricsDto.EventTypeMetrics> metricsMap = new ConcurrentHashMap<>();
        long total = totalEvents.get();

        eventsByType.forEach((eventType, count) -> {
            ComprehensiveMetricsDto.EventTypeMetrics typeMetrics =
                    new ComprehensiveMetricsDto.EventTypeMetrics();

            long countValue = count.get();
            typeMetrics.setCount(countValue);
            typeMetrics.setPercentage(total > 0 ? Math.round((countValue * 100.0 / total) * 100.0) / 100.0 : 0);

            // Calculate average latency for this type
            List<Long> latencies = latenciesByType.get(eventType);
            if (latencies != null && !latencies.isEmpty()) {
                double avgLatency = latencies.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0.0);
                typeMetrics.setAvgLatency(Math.round(avgLatency * 100.0) / 100.0);
            }

            metricsMap.put(eventType, typeMetrics);
        });

        return metricsMap;
    }

    private Map<String, ComprehensiveMetricsDto.SourceMetrics> buildSourceMetrics() {
        Map<String, ComprehensiveMetricsDto.SourceMetrics> metricsMap = new ConcurrentHashMap<>();

        eventsBySource.forEach((source, count) -> {
            ComprehensiveMetricsDto.SourceMetrics sourceMetrics =
                    new ComprehensiveMetricsDto.SourceMetrics();

            long countValue = count.get();
            sourceMetrics.setCount(countValue);

            // Calculate average latency for this source
            List<Long> latencies = latenciesBySource.get(source);
            if (latencies != null && !latencies.isEmpty()) {
                double avgLatency = latencies.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0.0);
                sourceMetrics.setAvgLatency(Math.round(avgLatency * 100.0) / 100.0);
            }

            // Error tracking for this source
            AtomicLong sourceErrors = errorsBySource.get(source);
            long errorCount = sourceErrors != null ? sourceErrors.get() : 0;
            sourceMetrics.setErrorCount(errorCount);
            sourceMetrics.setErrorRate(countValue > 0 ? Math.round((errorCount * 100.0 / countValue) * 100.0) / 100.0 : 0);

            // Determine health based on error rate and latency
            sourceMetrics.setHealth(determineSourceHealth(sourceMetrics.getErrorRate(), sourceMetrics.getAvgLatency()));

            metricsMap.put(source, sourceMetrics);
        });

        return metricsMap;
    }

    private ComprehensiveMetricsDto.UserMetrics buildUserMetrics() {
        ComprehensiveMetricsDto.UserMetrics userMetrics = new ComprehensiveMetricsDto.UserMetrics();

        userMetrics.setTotalUniqueUsers(eventsByUser.size());

        // Count active users in different time windows
        long now = System.currentTimeMillis();
        Set<String> activeUsersLast1Hour = new HashSet<>();
        Set<String> activeUsersLast24Hours = new HashSet<>();

        synchronized (eventTimestamps) {
            for (TimestampedEvent event : eventTimestamps) {
                if (event.userId != null) {
                    if (event.timestamp >= now - (60 * 60 * 1000)) {
                        activeUsersLast1Hour.add(event.userId);
                    }
                    if (event.timestamp >= now - (24 * 60 * 60 * 1000)) {
                        activeUsersLast24Hours.add(event.userId);
                    }
                }
            }
        }

        userMetrics.setActiveUsersLast1Hour(activeUsersLast1Hour.size());
        userMetrics.setActiveUsersLast24Hours(activeUsersLast24Hours.size());

        // Top active users
        List<ComprehensiveMetricsDto.UserMetrics.UserActivity> topUsers = eventsByUser.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
                .limit(10)
                .map(entry -> new ComprehensiveMetricsDto.UserMetrics.UserActivity(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toList());

        userMetrics.setTopActiveUsers(topUsers);

        return userMetrics;
    }

    private ComprehensiveMetricsDto.TopEventsMetrics buildTopEventsMetrics() {
        ComprehensiveMetricsDto.TopEventsMetrics topEvents = new ComprehensiveMetricsDto.TopEventsMetrics();

        // Most frequent events
        List<ComprehensiveMetricsDto.TopEventsMetrics.EventRanking> mostFrequent = eventsByType.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
                .limit(10)
                .map(entry -> {
                    String type = entry.getKey();
                    long count = entry.getValue().get();
                    double avgLatency = calculateAverageLatency(latenciesByType.get(type));
                    return new ComprehensiveMetricsDto.TopEventsMetrics.EventRanking(type, count, avgLatency);
                })
                .collect(Collectors.toList());
        topEvents.setMostFrequent(mostFrequent);

        // Fastest events (lowest latency)
        List<ComprehensiveMetricsDto.TopEventsMetrics.EventRanking> fastest = latenciesByType.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> {
                    String type = entry.getKey();
                    double avgLatency = calculateAverageLatency(entry.getValue());
                    long count = eventsByType.getOrDefault(type, new AtomicLong(0)).get();
                    return new ComprehensiveMetricsDto.TopEventsMetrics.EventRanking(type, count, avgLatency);
                })
                .sorted(Comparator.comparingDouble(ComprehensiveMetricsDto.TopEventsMetrics.EventRanking::getAvgLatency))
                .limit(5)
                .collect(Collectors.toList());
        topEvents.setFastest(fastest);

        // Slowest events (highest latency)
        List<ComprehensiveMetricsDto.TopEventsMetrics.EventRanking> slowest = latenciesByType.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> {
                    String type = entry.getKey();
                    double avgLatency = calculateAverageLatency(entry.getValue());
                    long count = eventsByType.getOrDefault(type, new AtomicLong(0)).get();
                    return new ComprehensiveMetricsDto.TopEventsMetrics.EventRanking(type, count, avgLatency);
                })
                .sorted((a, b) -> Double.compare(b.getAvgLatency(), a.getAvgLatency()))
                .limit(5)
                .collect(Collectors.toList());
        topEvents.setSlowest(slowest);

        return topEvents;
    }

    private ComprehensiveMetricsDto.ErrorAnalysisMetrics buildErrorAnalysisMetrics() {
        ComprehensiveMetricsDto.ErrorAnalysisMetrics errorAnalysis =
                new ComprehensiveMetricsDto.ErrorAnalysisMetrics();

        long totalErrorCount = errorsByType.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();

        errorAnalysis.setTotalErrors(totalErrorCount);

        long total = totalEvents.get();
        double errorRate = total > 0 ? (totalErrorCount * 100.0 / total) : 0;
        errorAnalysis.setErrorRate(Math.round(errorRate * 100.0) / 100.0);

        // Errors by type
        List<ComprehensiveMetricsDto.ErrorAnalysisMetrics.ErrorBreakdown> errorsByTypeList =
                errorsByType.entrySet().stream()
                        .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
                        .limit(10)
                        .map(entry -> {
                            long count = entry.getValue().get();
                            double percentage = totalErrorCount > 0 ? (count * 100.0 / totalErrorCount) : 0;
                            return new ComprehensiveMetricsDto.ErrorAnalysisMetrics.ErrorBreakdown(
                                    entry.getKey(),
                                    count,
                                    Math.round(percentage * 100.0) / 100.0
                            );
                        })
                        .collect(Collectors.toList());
        errorAnalysis.setErrorsByType(errorsByTypeList);

        // Errors by source
        List<ComprehensiveMetricsDto.ErrorAnalysisMetrics.ErrorBreakdown> errorsBySourceList =
                errorsBySource.entrySet().stream()
                        .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
                        .limit(10)
                        .map(entry -> {
                            long count = entry.getValue().get();
                            double percentage = totalErrorCount > 0 ? (count * 100.0 / totalErrorCount) : 0;
                            return new ComprehensiveMetricsDto.ErrorAnalysisMetrics.ErrorBreakdown(
                                    entry.getKey(),
                                    count,
                                    Math.round(percentage * 100.0) / 100.0
                            );
                        })
                        .collect(Collectors.toList());
        errorAnalysis.setErrorsBySource(errorsBySourceList);

        return errorAnalysis;
    }

    private ComprehensiveMetricsDto.PerformanceMetrics buildPerformanceMetrics() {
        ComprehensiveMetricsDto.PerformanceMetrics performance =
                new ComprehensiveMetricsDto.PerformanceMetrics();

        if (allLatencies.isEmpty()) {
            return performance;
        }

        List<Long> sortedLatencies = new ArrayList<>(allLatencies);
        Collections.sort(sortedLatencies);

        // Average
        double avgLatency = sortedLatencies.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        performance.setAvgLatency(Math.round(avgLatency * 100.0) / 100.0);

        // Percentiles
        performance.setP50(calculatePercentile(sortedLatencies, 50));
        performance.setP95(calculatePercentile(sortedLatencies, 95));
        performance.setP99(calculatePercentile(sortedLatencies, 99));

        // Min and Max
        performance.setMinLatency(sortedLatencies.get(0));
        performance.setMaxLatency(sortedLatencies.get(sortedLatencies.size() - 1));

        return performance;
    }

    private List<ComprehensiveMetricsDto.AnomalyAlert> detectAnomalies() {
        List<ComprehensiveMetricsDto.AnomalyAlert> alerts = new ArrayList<>();

        // Check for high error rate
        long totalErrorCount = errorsByType.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
        long total = totalEvents.get();
        double errorRate = total > 0 ? (totalErrorCount * 100.0 / total) : 0;

        if (errorRate > 5.0) { // More than 5% errors
            ComprehensiveMetricsDto.AnomalyAlert alert = new ComprehensiveMetricsDto.AnomalyAlert();
            alert.setSeverity(errorRate > 10 ? "critical" : "warning");
            alert.setType("high_error_rate");
            alert.setMessage("Error rate is above threshold");
            alert.setThreshold(5.0);
            alert.setCurrentValue(Math.round(errorRate * 100.0) / 100.0);
            alert.setDetectedAt(Instant.now());
            alerts.add(alert);
        }

        // Check for high latency
        if (!allLatencies.isEmpty()) {
            List<Long> sortedLatencies = new ArrayList<>(allLatencies);
            Collections.sort(sortedLatencies);
            double p95 = calculatePercentile(sortedLatencies, 95);

            if (p95 > 1000) { // P95 latency > 1 second
                ComprehensiveMetricsDto.AnomalyAlert alert = new ComprehensiveMetricsDto.AnomalyAlert();
                alert.setSeverity(p95 > 5000 ? "critical" : "warning");
                alert.setType("high_latency");
                alert.setMessage("P95 latency is above threshold");
                alert.setThreshold(1000.0);
                alert.setCurrentValue(p95);
                alert.setDetectedAt(Instant.now());
                alerts.add(alert);
            }
        }

        // Check for traffic spike
        long now = System.currentTimeMillis();
        long eventsLast1Min = countEventsInWindow(now, 60 * 1000);
        long eventsLast5Min = countEventsInWindow(now, 5 * 60 * 1000);
        double avgPerMinute = eventsLast5Min / 5.0;

        if (eventsLast1Min > avgPerMinute * 3) { // 3x spike
            ComprehensiveMetricsDto.AnomalyAlert alert = new ComprehensiveMetricsDto.AnomalyAlert();
            alert.setSeverity("warning");
            alert.setType("traffic_spike");
            alert.setMessage("Traffic spike detected (3x normal)");
            alert.setThreshold(avgPerMinute);
            alert.setCurrentValue(eventsLast1Min);
            alert.setDetectedAt(Instant.now());
            alerts.add(alert);
        }

        return alerts;
    }

    // ===== HELPER METHODS =====

    private long countEventsInWindow(long now, long windowMs) {
        long cutoff = now - windowMs;
        synchronized (eventTimestamps) {
            return eventTimestamps.stream()
                    .filter(event -> event.timestamp >= cutoff)
                    .count();
        }
    }

    private double calculateAverageLatency(List<Long> latencies) {
        if (latencies == null || latencies.isEmpty()) {
            return 0.0;
        }
        return Math.round(latencies.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0) * 100.0) / 100.0;
    }

    private double calculatePercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0.0;
        }
        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    private String calculateSystemHealth() {
        long totalErrorCount = errorsByType.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
        long total = totalEvents.get();
        double errorRate = total > 0 ? (totalErrorCount * 100.0 / total) : 0;

        if (errorRate > 10) {
            return "critical";
        } else if (errorRate > 5) {
            return "degraded";
        } else {
            return "healthy";
        }
    }

    private String determineSourceHealth(double errorRate, double avgLatency) {
        if (errorRate > 10 || avgLatency > 5000) {
            return "down";
        } else if (errorRate > 5 || avgLatency > 1000) {
            return "degraded";
        } else {
            return "healthy";
        }
    }

    /**
     * Reset all metrics
     */
    public synchronized void resetMetrics() {
        totalEvents.set(0);
        eventsByType.clear();
        eventsBySource.clear();
        eventsBySeverity.clear();
        eventsByUser.clear();
        latenciesByType.clear();
        latenciesBySource.clear();
        allLatencies.clear();
        errorsByType.clear();
        errorsBySource.clear();
        eventTimestamps.clear();
        lastMetricsTimestamp = System.currentTimeMillis();
        lastEventCount = 0;
        peakThroughput = 0.0;
        peakThroughputTimestamp = Instant.now();

        logger.info("All metrics reset");
    }


}
