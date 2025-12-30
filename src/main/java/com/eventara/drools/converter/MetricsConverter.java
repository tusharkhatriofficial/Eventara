package com.eventara.drools.converter;

import com.eventara.common.dto.ComprehensiveMetricsDto;
import com.eventara.drools.fact.MetricsFact;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MetricsConverter {

    /**
     * Convert ComprehensiveMetricsDto to MetricsFact for Drools
     */
    public MetricsFact convertToFact(ComprehensiveMetricsDto dto) {
        if (dto == null) {
            return null;
        }

        MetricsFact.MetricsFactBuilder builder = MetricsFact.builder();

        // Summary Metrics
        if (dto.getSummary() != null) {
            builder.totalEvents(dto.getSummary().getTotalEvents())
                    .uniqueSources(dto.getSummary().getUniqueSources())
                    .uniqueEventTypes(dto.getSummary().getUniqueEventTypes())
                    .uniqueUsers(dto.getSummary().getUniqueUsers())
                    .systemHealth(dto.getSummary().getSystemHealth())
                    .lastUpdated(dto.getSummary().getLastUpdated());
        }

        // Throughput Metrics
        if (dto.getThroughput() != null) {
            if (dto.getThroughput().getCurrent() != null) {
                builder.eventsPerSecond(dto.getThroughput().getCurrent().getPerSecond())
                        .eventsPerMinute(dto.getThroughput().getCurrent().getPerMinute())
                        .eventsPerHour(dto.getThroughput().getCurrent().getPerHour())
                        .eventsPerDay(dto.getThroughput().getCurrent().getPerDay());
            }

            if (dto.getThroughput().getPeak() != null) {
                builder.peakThroughput(dto.getThroughput().getPeak().getValue())
                        .peakThroughputTime(dto.getThroughput().getPeak().getTimestamp());
            }

            if (dto.getThroughput().getAverage() != null) {
                builder.avgThroughputLast1Hour(dto.getThroughput().getAverage().getLast1Hour())
                        .avgThroughputLast24Hours(dto.getThroughput().getAverage().getLast24Hours());
            }
        }

        // Time Window Metrics
        if (dto.getTimeWindows() != null) {
            builder.eventsLast1Minute(dto.getTimeWindows().getLast1Minute())
                    .eventsLast5Minutes(dto.getTimeWindows().getLast5Minutes())
                    .eventsLast15Minutes(dto.getTimeWindows().getLast15Minutes())
                    .eventsLast1Hour(dto.getTimeWindows().getLast1Hour())
                    .eventsLast24Hours(dto.getTimeWindows().getLast24Hours());
        }

        // Error Metrics
        if (dto.getErrorAnalysis() != null) {
            builder.totalErrors(dto.getErrorAnalysis().getTotalErrors())
                    .errorRate(dto.getErrorAnalysis().getErrorRate());
        }

        // Performance Metrics
        if (dto.getPerformance() != null) {
            builder.avgLatency(dto.getPerformance().getAvgLatency())
                    .p50Latency(dto.getPerformance().getP50())
                    .p95Latency(dto.getPerformance().getP95())
                    .p99Latency(dto.getPerformance().getP99())
                    .maxLatency(dto.getPerformance().getMaxLatency())
                    .minLatency(dto.getPerformance().getMinLatency());
        }

        // User Metrics
        if (dto.getUserMetrics() != null) {
            builder.totalUniqueUsers(dto.getUserMetrics().getTotalUniqueUsers())
                    .activeUsersLast1Hour(dto.getUserMetrics().getActiveUsersLast1Hour())
                    .activeUsersLast24Hours(dto.getUserMetrics().getActiveUsersLast24Hours());
        }

        // Set timestamp
        builder.timestamp(Instant.now());

        return builder.build();
    }
}
