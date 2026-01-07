package com.eventara.analytics.service;

import com.eventara.drools.fact.MetricsFact;
import com.eventara.common.repository.EventRepository;
import com.eventara.ingestion.model.entity.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates MetricsFact for Drools rule evaluation using REAL data from PostgreSQL/TimescaleDB.
 * This ensures rules are evaluated against accurate, persistent data (not volatile in-memory data).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsCalculator {

    private final EventRepository eventRepository;

    public MetricsFact calculateCurrentMetrics() {
        Instant now = Instant.now();

        // Define time windows
        Instant oneMinuteAgo = now.minusSeconds(60);
        Instant fiveMinutesAgo = now.minusSeconds(300);
        Instant fifteenMinutesAgo = now.minusSeconds(900);
        Instant oneHourAgo = now.minusSeconds(3600);
        Instant twentyFourHoursAgo = now.minusSeconds(86400);

        // ===== COUNT EVENTS IN TIME WINDOWS =====
        long eventsLast1Minute = eventRepository.countByTimestampBetween(oneMinuteAgo, now);
        long eventsLast5Minutes = eventRepository.countByTimestampBetween(fiveMinutesAgo, now);
        long eventsLast15Minutes = eventRepository.countByTimestampBetween(fifteenMinutesAgo, now);
        long eventsLast1Hour = eventRepository.countByTimestampBetween(oneHourAgo, now);
        long eventsLast24Hours = eventRepository.countByTimestampBetween(twentyFourHoursAgo, now);

        // ===== COUNT ERRORS (using CRITICAL + ERROR severity) =====
        long criticalErrors = eventRepository.countBySeverityAndTimestampBetween(Event.Severity.CRITICAL, twentyFourHoursAgo, now);
        long regularErrors = eventRepository.countBySeverityAndTimestampBetween(Event.Severity.ERROR, twentyFourHoursAgo, now);
        long totalErrors = criticalErrors + regularErrors;

        // ===== CALCULATE ERROR RATE =====
        double errorRate = eventsLast24Hours > 0 ? (totalErrors * 100.0 / eventsLast24Hours) : 0.0;

        // ===== UNIQUE COUNTS =====
        int uniqueSources = eventRepository.countDistinctSourceByTimestampBetween(twentyFourHoursAgo, now);
        int uniqueEventTypes = eventRepository.countDistinctEventTypeByTimestampBetween(twentyFourHoursAgo, now);
        int uniqueUsers = eventRepository.countDistinctUserIdByTimestampBetween(twentyFourHoursAgo, now);
        int activeUsersLast1Hour = eventRepository.countDistinctUserIdByTimestampBetween(oneHourAgo, now);

        // ===== LATENCY METRICS (from DB - accurate for rule evaluation) =====
        Double avgLatency = eventRepository.findAvgLatencyBetween(oneHourAgo, now);
        Double p50Latency = eventRepository.findP50LatencyBetween(oneHourAgo, now);
        Double p95Latency = eventRepository.findP95LatencyBetween(oneHourAgo, now);
        Double p99Latency = eventRepository.findP99LatencyBetween(oneHourAgo, now);
        Double minLatency = eventRepository.findMinLatencyBetween(oneHourAgo, now);
        Double maxLatency = eventRepository.findMaxLatencyBetween(oneHourAgo, now);

        // ===== THROUGHPUT CALCULATIONS =====
        double eventsPerSecond = eventsLast1Minute / 60.0;
        double eventsPerMinute = (double) eventsLast1Minute;
        double eventsPerHour = (double) eventsLast1Hour;
        double eventsPerDay = (double) eventsLast24Hours;
        double avgThroughputLast1Hour = eventsLast1Hour / 3600.0;
        double avgThroughputLast24Hours = eventsLast24Hours / 86400.0;

        // ===== SYSTEM HEALTH (based on error rate and latency) =====
        String systemHealth = determineSystemHealth(errorRate, p95Latency);

        // ===== SOURCE-LEVEL METRICS =====
        Map<String, String> sourceHealthMap = new HashMap<>();
        Map<String, Double> sourceErrorRateMap = new HashMap<>();
        Map<String, Long> sourceErrorCountMap = new HashMap<>();

        List<String> sources = eventRepository.findDistinctSourcesByTimestampBetween(oneHourAgo, now);
        for (String source : sources) {
            long sourceEventCount = eventRepository.countBySourceAndTimestampBetween(source, oneHourAgo, now);
            long sourceCriticalErrors = eventRepository.countBySourceAndSeverityAndTimestampBetween(source, Event.Severity.CRITICAL, oneHourAgo, now);
            long sourceRegularErrors = eventRepository.countBySourceAndSeverityAndTimestampBetween(source, Event.Severity.ERROR, oneHourAgo, now);
            long sourceErrors = sourceCriticalErrors + sourceRegularErrors;

            double sourceErrorRate = sourceEventCount > 0 ? (sourceErrors * 100.0 / sourceEventCount) : 0.0;
            Double sourceAvgLatency = eventRepository.findAvgLatencyBySourceBetween(source, oneHourAgo, now);

            sourceErrorCountMap.put(source, sourceErrors);
            sourceErrorRateMap.put(source, sourceErrorRate);
            sourceHealthMap.put(source, determineSourceHealth(sourceErrorRate, sourceAvgLatency));
        }

        // ===== BUILD METRICS FACT =====
        MetricsFact metrics = MetricsFact.builder()
                // Summary
                .totalEvents(eventsLast24Hours)
                .uniqueSources(uniqueSources)
                .uniqueEventTypes(uniqueEventTypes)
                .uniqueUsers(uniqueUsers)
                .systemHealth(systemHealth)
                .lastUpdated(now)

                // Throughput
                .eventsPerSecond(eventsPerSecond)
                .eventsPerMinute(eventsPerMinute)
                .eventsPerHour(eventsPerHour)
                .eventsPerDay(eventsPerDay)
                .avgThroughputLast1Hour(avgThroughputLast1Hour)
                .avgThroughputLast24Hours(avgThroughputLast24Hours)

                // Time Windows
                .eventsLast1Minute(eventsLast1Minute)
                .eventsLast5Minutes(eventsLast5Minutes)
                .eventsLast15Minutes(eventsLast15Minutes)
                .eventsLast1Hour(eventsLast1Hour)
                .eventsLast24Hours(eventsLast24Hours)

                // Errors
                .totalErrors(totalErrors)
                .errorRate(errorRate)

                // Performance/Latency
                .avgLatency(avgLatency != null ? avgLatency : 0.0)
                .p50Latency(p50Latency != null ? p50Latency : 0.0)
                .p95Latency(p95Latency != null ? p95Latency : 0.0)
                .p99Latency(p99Latency != null ? p99Latency : 0.0)
                .minLatency(minLatency != null ? minLatency : 0.0)
                .maxLatency(maxLatency != null ? maxLatency : 0.0)

                // Users
                .totalUniqueUsers((long) uniqueUsers)
                .activeUsersLast1Hour((long) activeUsersLast1Hour)
                .activeUsersLast24Hours((long) uniqueUsers)

                // Source Maps
                .sourceHealthMap(sourceHealthMap)
                .sourceErrorRateMap(sourceErrorRateMap)
                .sourceErrorCountMap(sourceErrorCountMap)

                // Metadata
                .timestamp(now)
                .build();

        log.debug("Calculated metrics from DB: totalEvents={}, errorRate={}%, systemHealth={}, sources={}",
                eventsLast24Hours, String.format("%.2f", errorRate), systemHealth, uniqueSources);

        return metrics;
    }

    /**
     * Determine overall system health based on error rate and latency
     */
    private String determineSystemHealth(double errorRate, Double p95Latency) {
        if (errorRate > 10.0 || (p95Latency != null && p95Latency > 5000)) {
            return "critical";
        } else if (errorRate > 5.0 || (p95Latency != null && p95Latency > 2000)) {
            return "degraded";
        }
        return "healthy";
    }

    /**
     * Determine source health based on error rate and latency
     */
    private String determineSourceHealth(double errorRate, Double avgLatency) {
        if (errorRate > 10.0 || (avgLatency != null && avgLatency > 3000)) {
            return "down";
        } else if (errorRate > 5.0 || (avgLatency != null && avgLatency > 1000)) {
            return "degraded";
        }
        return "healthy";
    }
}
