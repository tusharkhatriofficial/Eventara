package com.eventara.drools.fact;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Fact object representing current metrics for Drools rule evaluation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsFact implements Serializable {

    // Error Metrics
    private Double errorRate;
    private Long errorCount;

    // Performance Metrics
    private Double avgLatency;
    private Double p50Latency;
    private Double p95Latency;
    private Double p99Latency;
    private Double maxLatency;

    // Throughput Metrics
    private Double eventsPerSecond;
    private Double eventsPerMinute;

    // Time Window Metrics
    private Long eventsLast1Minute;
    private Long eventsLast5Minutes;
    private Long eventsLast15Minutes;
    private Long eventsLast1Hour;
    private Long eventsLast24Hours;

    // Source Metrics
    private String sourceHealth;
    private Double sourceErrorRate;
    private Double sourceLatency;

    // User Metrics
    private Long activeUsers1Hour;
    private Long activeUsers24Hours;
    private Long totalUniqueUsers;

    // System Metrics
    private String systemHealth;
    private Long uniqueSources;
    private Long uniqueEventTypes;

    // Metadata
    private Long timestamp;
}
