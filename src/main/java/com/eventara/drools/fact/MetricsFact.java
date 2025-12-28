package com.eventara.drools.fact;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Fact object for Drools based on ComprehensiveMetricsDto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsFact implements Serializable {

    // ===== SUMMARY METRICS =====
    private Long totalEvents;
    private Integer uniqueSources;
    private Integer uniqueEventTypes;
    private Integer uniqueUsers;
    private String systemHealth; // "healthy", "degraded", "critical"
    private Instant lastUpdated;

    // ===== THROUGHPUT METRICS =====
    private Double eventsPerSecond;
    private Double eventsPerMinute;
    private Double eventsPerHour;
    private Double eventsPerDay;
    private Double peakThroughput;
    private Instant peakThroughputTime;
    private Double avgThroughputLast1Hour;
    private Double avgThroughputLast24Hours;

    // ===== TIME WINDOW METRICS =====
    private Long eventsLast1Minute;
    private Long eventsLast5Minutes;
    private Long eventsLast15Minutes;
    private Long eventsLast1Hour;
    private Long eventsLast24Hours;

    // ===== ERROR METRICS =====
    private Long totalErrors;
    private Double errorRate;

    // ===== PERFORMANCE METRICS =====
    private Double avgLatency;
    private Double p50Latency;
    private Double p95Latency;
    private Double p99Latency;
    private Double maxLatency;
    private Double minLatency;

    // ===== USER METRICS =====
    private Long totalUniqueUsers;
    private Long activeUsersLast1Hour;
    private Long activeUsersLast24Hours;

    // ===== SOURCE HEALTH =====
    private Map<String, String> sourceHealthMap; // sourceName -> "healthy"/"degraded"/"down"
    private Map<String, Double> sourceErrorRateMap;
    private Map<String, Long> sourceErrorCountMap;

    // ===== METADATA =====
    private Instant timestamp;
}


//package com.eventara.drools.fact;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.io.Serializable;
//
///**
// * Fact object representing current metrics for Drools rule evaluation
// */
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class MetricsFact implements Serializable {
//
//    // Error Metrics
//    private Double errorRate;
//    private Long errorCount;
//
//    // Performance Metrics
//    private Double avgLatency;
//    private Double p50Latency;
//    private Double p95Latency;
//    private Double p99Latency;
//    private Double maxLatency;
//
//    // Throughput Metrics
//    private Double eventsPerSecond;
//    private Double eventsPerMinute;
//
//    // Time Window Metrics
//    private Long eventsLast1Minute;
//    private Long eventsLast5Minutes;
//    private Long eventsLast15Minutes;
//    private Long eventsLast1Hour;
//    private Long eventsLast24Hours;
//
//    // Source Metrics
//    private String sourceHealth;
//    private Double sourceErrorRate;
//    private Double sourceLatency;
//
//    // User Metrics
//    private Long activeUsers1Hour;
//    private Long activeUsers24Hours;
//    private Long totalUniqueUsers;
//
//    // System Metrics
//    private String systemHealth;
//    private Long uniqueSources;
//    private Long uniqueEventTypes;
//
//    // Metadata
//    private Long timestamp;
//}